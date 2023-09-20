package org.andes.lock.core;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.*;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class AutoLockProcessor extends AbstractProcessor {
    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        new AutoLockBuilder().process(processingEnv, roundEnv);
        return true;
    }


}

class AutoLockBuilder {
    private ProcessingEnvironment processingEnv;

    void process(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv) {
        this.processingEnv = processingEnv;
        Set<? extends Element> elements = roundEnv.getRootElements();
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            if (element.getSimpleName().toString().endsWith("$Proxy")) {
                continue;
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, """
                    parse class:
                        
                    """ + element);
            var typeElement = (TypeElement) element;
            try {
                if (hasMethodWithAnnotation(typeElement)) {
                    parseElement(typeElement);
                }
            } catch (Exception e) {
                processingEnv.getMessager().printError(e.toString());
            }

        }
    }

    /**
     * 是否存在相应方法
     */
    boolean hasMethodWithAnnotation(TypeElement element) {
        return element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                // 去除私有
                .filter(e -> !e.getModifiers().contains(Modifier.PRIVATE) &&
                        !e.getModifiers().contains(Modifier.STATIC) &&
                        !e.getModifiers().contains(Modifier.FINAL))
                .anyMatch(this::methodHasAnnotation);
    }

    void parseElement(TypeElement element) throws Exception {
        var name = element.toString();
        var packageName = name.substring(0, name.lastIndexOf('.'));
        var enclosedElements = element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                // 去除私有
                .filter(e -> !e.getModifiers().contains(Modifier.PRIVATE) &&
                        !e.getModifiers().contains(Modifier.STATIC) &&
                        !e.getModifiers().contains(Modifier.FINAL))
                .toList();
        String sb = "package " + packageName + ";\n\n" +
                """
                        import org.andes.lock.core.*;
                        import java.util.*;
                                                
                        """ +
                buildAnnotation(element, "") +
                String.format("""
                                public class %s$Proxy extends %s implements AutoLockProxy {
                                                        
                                %s
                                %s
                                }
                                                        
                                """, element.getSimpleName().toString(), name,
                        buildConstruct(name),
                        buildOverrideMethod(enclosedElements));

        JavaFileObject source = processingEnv.getFiler().createSourceFile(name + "$Proxy");
        Writer writer = source.openWriter();
        writer.write(sb);
        writer.flush();
        writer.close();
    }

    /**
     * 覆盖注解
     */
    private String buildAnnotation(Element element, String prefix) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors()
                .stream()
                // 取消spring
                .filter(e -> !e.getAnnotationType().toString().startsWith("org.springframework.stereotype."))
                .toList();
        var sb = new StringBuilder();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            var elementValues = annotationMirror.getElementValues();
            if (annotationMirror.getAnnotationType().toString().equals(AutoLock.class.getCanonicalName())) {
                sb.append(prefix).append("@org.andes.lock.core.annotations.AutoLock").append("(");
            } else {
                sb.append(prefix).append("@").append(annotationMirror.getAnnotationType().toString()).append("(");
            }
            var list = elementValues.keySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i).getSimpleName()).append(" = ").append(elementValues.get(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    private String buildFieldAnnotation(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors()
                .stream()
                .toList();
        var sb = new StringBuilder();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            var elementValues = annotationMirror.getElementValues();
            sb.append("@").append(annotationMirror.getAnnotationType().toString()).append("(");
            var list = elementValues.keySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i).getSimpleName()).append(" = ").append(elementValues.get(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(") ");
        }
        return sb.toString();
    }

    private boolean methodHasAnnotation(ExecutableElement element) {
        var set = new HashSet<String>();
        return methodHasAnnotation(element, set);
    }

    private boolean methodHasAnnotation(ExecutableElement element, Set<String> excludeProperties) {
        var annotation = element.getAnnotationMirrors()
                .stream()
                .filter(e -> e.getAnnotationType().toString().equals(AutoLock.class.getCanonicalName()))
                .findFirst()
                .orElse(null);
        if (annotation == null) {
            return false;
        }

        var mirrors = annotation.getElementValues();
        if (mirrors != null && !mirrors.isEmpty()) {
            var values = mirrors.values().stream().toList();
            AnnotationValue annotationValue = values.get(0);
            var name = annotationValue.toString();
            if (name.startsWith("{")) {
                name = name.substring(1, name.length() - 1);
            }
            excludeProperties.addAll(Arrays.asList(name.split(",\\s+")));
        }
        excludeProperties.remove("");
        return true;
    }

    /**
     * 覆盖方法
     */
    private String buildOverrideMethod(List<ExecutableElement> elements) {
        var sb = new StringBuilder();
        for (ExecutableElement element : elements) {
            var set = new HashSet<String>();
            if (!methodHasAnnotation(element, set)) {
                continue;
            }
            var simpleName = element.getSimpleName();
            var modifiers = element.getModifiers();
            var parameters = element.getParameters();
            var returnType = element.getReturnType();
            sb.append(buildAnnotation(element, "\t"));
            sb.append("\t");
            if (modifiers.contains(Modifier.PUBLIC)) {
                sb.append("public ");
            } else if (modifiers.contains(Modifier.PROTECTED)) {
                sb.append("protected ");
            }
            sb.append(returnType.toString()).append(" ").append(simpleName).append("(");
            StringBuilder params = new StringBuilder();
            var paramNames = new LinkedHashSet<String>();
            for (int i = 0; i < parameters.size(); i++) {
                var variableElement = parameters.get(i);
                sb.append(buildFieldAnnotation(variableElement))
                        .append(variableElement.asType().toString()).append(" ")
                        .append(variableElement.getSimpleName());
                params.append(variableElement.getSimpleName());
                paramNames.add(variableElement.getSimpleName().toString());
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                    params.append(", ");
                }
            }
            sb.append(") {\n");
            paramNames.removeIf(set::contains);
            var ps = String.join(", ", paramNames);
            if (returnType.getKind() == TypeKind.VOID) {
                sb.append(String.format("""
                                var chain = manager.getLockChain(%s);
                                chain.lock();
                                try {
                                    _raw.%s(%s);
                                } finally {
                                    chain.unlock();
                                }
                            
                        """, ps, simpleName, parameters));
            } else {
                sb.append(String.format("""
                                var chain = manager.getLockChain(%s);
                                chain.lock();
                                try {
                                    return _raw.%s(%s);
                                } finally {
                                    chain.unlock();
                                }
                            
                        """, ps, simpleName, parameters));
            }
            sb.append("\t}\n\n");
        }
        return sb.toString();
    }

    /**
     * 新增构造方法
     */
    private String buildConstruct(String name) {
        var simpleName = name.substring(name.lastIndexOf('.') + 1);

        return String.format("""
                    public final %s _raw;
                    public final LockManager manager;
                    public %s$Proxy( %s _raw, LockManager manager){
                        this._raw = _raw;
                        this.manager = manager;
                    }
                    
                """, name, simpleName, name);
    }

}