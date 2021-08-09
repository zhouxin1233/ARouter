package com.alibaba.android.arouter.compiler.processor;

import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_AUTOWIRED;
import static com.alibaba.android.arouter.compiler.utils.Consts.ISYRINGE;
import static com.alibaba.android.arouter.compiler.utils.Consts.JSON_SERVICE;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_INJECT;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_AUTOWIRED;
import static com.alibaba.android.arouter.compiler.utils.Consts.TYPE_WRAPPER;
import static com.alibaba.android.arouter.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Processor used to create autowired helper
 * 用于处理@Autowired这个注解
 * AutowiredProcessor用来生成各种以 Autowired为后缀的帮助类文件
 *
 * 这些帮助类最后会在运行时被用来进行成员变量的自动注入
 * 使用帮助类而不是运行时直接反射对成员变量赋值，避免了反射过多的开销
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({ANNOTATION_TYPE_AUTOWIRED})
public class AutowiredProcessor extends BaseProcessor {
    // 储存着 (路由节点 - 需要自动注入的参数）的映射关系
    private final Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();   // Contain field need autowired and his super class.
    private static final ClassName ARouterClass = ClassName.get("com.alibaba.android.arouter.launcher", "ARouter");
    private static final ClassName AndroidLog = ClassName.get("android.util", "Log");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        logger.info(">>> AutowiredProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            try {
                logger.info(">>> Found autowired field, start... <<<");
                // 所有传进来的element进行扫描，将它们所在的路由节点和它们的映射关系存到上文提到的parentAndChild参数中
                categories(roundEnvironment.getElementsAnnotatedWith(Autowired.class));
                // 生成帮助类文件
                generateHelper();

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void generateHelper() throws IOException, IllegalAccessException {
        TypeElement type_ISyringe = elementUtils.getTypeElement(ISYRINGE);
        TypeElement type_JsonService = elementUtils.getTypeElement(JSON_SERVICE);
        TypeMirror iProvider = elementUtils.getTypeElement(Consts.IPROVIDER).asType();
        TypeMirror activityTm = elementUtils.getTypeElement(Consts.ACTIVITY).asType();
        TypeMirror fragmentTm = elementUtils.getTypeElement(Consts.FRAGMENT).asType();
        TypeMirror fragmentTmX = elementUtils.getTypeElement(Consts.FRAGMENT_X).asType();

        // JavaPoet语法 构建帮助类的inject方法的形参Object target / Build input param name.
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();

        if (MapUtils.isNotEmpty(parentAndChild)) {
            // 遍历parentAndChild,每一次循环生成一个帮助类文件，即为每一个含有自动注入变量的路由节点都生成一个帮助类
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                //  JavaPoet语法，构建public inject(Object target)方法 / Build method : 'inject'
                MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder(METHOD_INJECT)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(objectParamSpec);

                TypeElement parent = entry.getKey();
                List<Element> childs = entry.getValue();

                String qualifiedName = parent.getQualifiedName().toString();
                String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
                String fileName = parent.getSimpleName() + NAME_OF_AUTOWIRED;

                logger.info(">>> Start process " + childs.size() + " field in " + parent.getSimpleName() + " ... <<<");

                // JavaPoet 语法,构建帮助类,修饰符为public,并实现ISyringe接口
                TypeSpec.Builder helper = TypeSpec.classBuilder(fileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(ClassName.get(type_ISyringe))
                        .addModifiers(PUBLIC);

                // JavaPoet 语法，构建成员变量private SerializationService serializationService
                // 这个成员变量是用于序列化自定义的对象
                FieldSpec jsonServiceField = FieldSpec.builder(TypeName.get(type_JsonService.asType()), "serializationService", Modifier.PRIVATE).build();
                helper.addField(jsonServiceField);

                //构建inject方法体serializationService = ARouter.getInstance().navigation(SerializationService.class);
                //可以看到实例的获得是通过Arouter获得服务的方式
                injectMethodBuilder.addStatement("serializationService = $T.getInstance().navigation($T.class)", ARouterClass, ClassName.get(type_JsonService));
                injectMethodBuilder.addStatement("$T substitute = ($T)target", ClassName.get(parent), ClassName.get(parent));

                // 构建inject方法体，遍历childs，为每一个childs添加赋值语句 Generate method body, start inject.
                for (Element element : childs) {
                    Autowired fieldConfig = element.getAnnotation(Autowired.class);
                    String fieldName = element.getSimpleName().toString();

                    //如果这个成员变量是IPrivider 的实现类（即自定义的各种服务），就通过ARouter获取服务的方式去获得实例
                    //如果在@Autowired这个注解中指定了name，则通过byName方式,否则通过byType方式
                    if (types.isSubtype(element.asType(), iProvider)) {  // It's provider
                        if ("".equals(fieldConfig.name())) {    // User has not set service path, then use byType.

                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = $T.getInstance().navigation($T.class)",
                                    ARouterClass,
                                    ClassName.get(element.asType())
                            );
                        } else {    // use byName
                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = ($T)$T.getInstance().build($S).navigation()",
                                    ClassName.get(element.asType()),
                                    ARouterClass,
                                    fieldConfig.name()
                            );
                        }

                        // Validator   如果需要进行非空检验，则添加非空检验的语法
                        if (fieldConfig.required()) {
                            injectMethodBuilder.beginControlFlow("if (substitute." + fieldName + " == null)");
                            injectMethodBuilder.addStatement(
                                    "throw new RuntimeException(\"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    } else {    // It's normal intent value
                        // 这个成员变量不是IProvider
                        // 通过isActivity标记目标路由节点是不是Activity
                        String originalValue = "substitute." + fieldName;
                        String statement = "substitute." + fieldName + " = " + buildCastCode(element) + "substitute.";
                        boolean isActivity = false;
                        if (types.isSubtype(parent.asType(), activityTm)) {  // Activity, then use getIntent()
                            // 是Activity就通过getIntent()的方式获得传递过来的参数
                            isActivity = true;
                            statement += "getIntent().";
                        } else if (types.isSubtype(parent.asType(), fragmentTm) || types.isSubtype(parent.asType(), fragmentTmX)) {   // Fragment, then use getArguments()
                            // 是fragment就通过getArguments()的方式获得传递过来的参数
                            statement += "getArguments().";
                        } else {
                            throw new IllegalAccessException("The field [" + fieldName + "] need autowired from intent, its parent must be activity or fragment!");
                        }

                        //JavaPoet语法
                        statement = buildStatement(originalValue, statement, typeUtils.typeExchange(element), isActivity, isKtClass(parent));
                        if (statement.startsWith("serializationService.")) {   // Not mortals
                            injectMethodBuilder.beginControlFlow("if (null != serializationService)");
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = " + statement,
                                    (StringUtils.isEmpty(fieldConfig.name()) ? fieldName : fieldConfig.name()),
                                    ClassName.get(element.asType())
                            );
                            injectMethodBuilder.nextControlFlow("else");
                            injectMethodBuilder.addStatement(
                                    "$T.e(\"" + Consts.TAG + "\", \"You want automatic inject the field '" + fieldName + "' in class '$T' , then you should implement 'SerializationService' to support object auto inject!\")", AndroidLog, ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        } else {
                            injectMethodBuilder.addStatement(statement, StringUtils.isEmpty(fieldConfig.name()) ? fieldName : fieldConfig.name());
                        }

                        // Validator 如果需要进行非空检验，则添加非空检验的语法
                        if (fieldConfig.required() && !element.asType().getKind().isPrimitive()) {  // Primitive wont be check.
                            injectMethodBuilder.beginControlFlow("if (null == substitute." + fieldName + ")");
                            injectMethodBuilder.addStatement(
                                    "$T.e(\"" + Consts.TAG + "\", \"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", AndroidLog, ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    }
                }

                //JavaPoet语法
                helper.addMethod(injectMethodBuilder.build());

                // Generate autowire helper 输出帮助类的java文件
                JavaFile.builder(packageName, helper.build()).build().writeTo(mFiler);

                logger.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
            }

            logger.info(">>> Autowired processor stop. <<<");
        }
    }

    private boolean isKtClass(Element element) {
        for (AnnotationMirror annotationMirror : elementUtils.getAllAnnotationMirrors(element)) {
            if (annotationMirror.getAnnotationType().toString().contains("kotlin")) {
                return true;
            }
        }

        return false;
    }

    private String buildCastCode(Element element) {
        if (typeUtils.typeExchange(element) == TypeKind.SERIALIZABLE.ordinal()) {
            return CodeBlock.builder().add("($T) ", ClassName.get(element.asType())).build().toString();
        }
        return "";
    }

    /**
     * Build param inject statement
     */
    private String buildStatement(String originalValue, String statement, int type, boolean isActivity, boolean isKt) {
        switch (TypeKind.values()[type]) {
            case BOOLEAN:
                statement += "getBoolean" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case BYTE:
                statement += "getByte" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case SHORT:
                statement += "getShort" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case INT:
                statement += "getInt" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case LONG:
                statement += "getLong" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case CHAR:
                statement += "getChar" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case FLOAT:
                statement += "getFloat" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case DOUBLE:
                statement += "getDouble" + (isActivity ? "Extra" : "") + "($S, " + originalValue + ")";
                break;
            case STRING:
                statement += (isActivity ? ("getExtras() == null ? " + originalValue + " : substitute.getIntent().getExtras().getString($S") : ("getString($S")) + ", " + originalValue + ")";
                break;
            case SERIALIZABLE:
                statement += (isActivity ? ("getSerializableExtra($S)") : ("getSerializable($S)"));
                break;
            case PARCELABLE:
                statement += (isActivity ? ("getParcelableExtra($S)") : ("getParcelable($S)"));
                break;
            case OBJECT:
                statement = "serializationService.parseObject(substitute." + (isActivity ? "getIntent()." : "getArguments().") + (isActivity ? "getStringExtra($S)" : "getString($S)") + ", new " + TYPE_WRAPPER + "<$T>(){}.getType())";
                break;
        }

        return statement;
    }

    /**
     * Categories field, find his papa.
     * 就是对所有传进来的element进行扫描，将它们所在的路由节点和它们的映射关系存到 的parentAndChild参数中
     *
     * @param elements Field need autowired
     */
    private void categories(Set<? extends Element> elements) throws IllegalAccessException {
        if (CollectionUtils.isNotEmpty(elements)) {
            for (Element element : elements) {
                TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

                // 被标记的成员变量不能为private，否则抛出异常
                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new IllegalAccessException("The inject fields CAN NOT BE 'private'!!! please check field ["
                            + element.getSimpleName() + "] in class [" + enclosingElement.getQualifiedName() + "]");
                }

                // 将映射关系添加到parentAndChild中
                if (parentAndChild.containsKey(enclosingElement)) { // Has categries
                    parentAndChild.get(enclosingElement).add(element);
                } else {
                    List<Element> childs = new ArrayList<>();
                    childs.add(element);
                    parentAndChild.put(enclosingElement, childs);
                }
            }

            logger.info("categories finished.");
        }
    }
}
