package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.facade.annotation.Interceptor;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.alibaba.android.arouter.compiler.utils.Consts.*;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Process the annotation of #{@link Interceptor}
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 14:11
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes(ANNOTATION_TYPE_INTECEPTOR)
public class InterceptorProcessor extends BaseProcessor {
    // 存放着优先级和拦截器的映射关系, TreeMap默认会按照key进行排序，所以这就实现了拦截器按优先级进行排序
    private Map<Integer, Element> interceptors = new TreeMap<>();
    private TypeMirror iInterceptor = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        iInterceptor = elementUtils.getTypeElement(Consts.IINTERCEPTOR).asType();

        logger.info(">>> InterceptorProcessor init. <<<");
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Interceptor.class);
            try {
                parseInterceptors(elements);
            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    /**
     * Parse tollgate.
     *
     * @param elements elements of tollgate.
     */
    private void parseInterceptors(Set<? extends Element> elements) throws IOException {
        if (CollectionUtils.isNotEmpty(elements)) {
            logger.info(">>> Found interceptors, size is " + elements.size() + " <<<");

            // Verify and cache, sort incidentally.
            for (Element element : elements) {
                // verify 方法很简单,就是判断传入的element有没有@Interceptor注解并实现了IIntercptor接口
                if (verify(element)) {  // Check the interceptor meta
                    logger.info("A interceptor verify over, its " + element.asType());
                    Interceptor interceptor = element.getAnnotation(Interceptor.class);

                    Element lastInterceptor = interceptors.get(interceptor.priority());
                    // 如果已添加过相同优先级的拦截器，则抛出异常
                    if (null != lastInterceptor) { // Added, throw exceptions
                        throw new IllegalArgumentException(
                                String.format(Locale.getDefault(), "More than one interceptors use same priority [%d], They are [%s] and [%s].",
                                        interceptor.priority(),
                                        lastInterceptor.getSimpleName(),
                                        element.getSimpleName())
                        );
                    }

                    // 将拦截器的优先级和拦截器的映射关系添加进去
                    interceptors.put(interceptor.priority(), element);
                } else {
                    logger.error("A interceptor verify failed, its " + element.asType());
                }
            }

            //  分别为IInterceptor和IInterceptorGroup接口的TypeElement实例 / Interface of ARouter.
            TypeElement type_ITollgate = elementUtils.getTypeElement(IINTERCEPTOR);
            TypeElement type_ITollgateGroup = elementUtils.getTypeElement(IINTERCEPTOR_GROUP);

            /**
             * JavaPoet语法，构建形参类型Map<Integer, Class<? extends ITollgate>>
             *  Build input type, format as :
             *
             *  ```Map<Integer, Class<? extends ITollgate>>```
             */
            ParameterizedTypeName inputMapTypeOfTollgate = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Integer.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_ITollgate))
                    )
            );

            // JavaPoet语法，构建形参名 Build input param name.
            ParameterSpec tollgateParamSpec = ParameterSpec.builder(inputMapTypeOfTollgate, "interceptors").build();

            // JavaPoet语法，构建loadinto方法 Build method : 'loadInto'
            MethodSpec.Builder loadIntoMethodOfTollgateBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(tollgateParamSpec);

            // Generate  构建方法体，为每一个interceptor都添加一条类似interceptors.put(7, Test1Interceptor.class)的语句
            if (null != interceptors && interceptors.size() > 0) {
                // Build method body
                for (Map.Entry<Integer, Element> entry : interceptors.entrySet()) {
                    loadIntoMethodOfTollgateBuilder.addStatement("interceptors.put(" + entry.getKey() + ", $T.class)", ClassName.get((TypeElement) entry.getValue()));
                }
            }

            //  生成Arouter$$Interceptors$$(模块名) 文件  Write to disk(Write file even interceptors is empty.)
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(NAME_OF_INTERCEPTOR + SEPARATOR + moduleName)
                            .addModifiers(PUBLIC)
                            .addJavadoc(WARNING_TIPS)
                            .addMethod(loadIntoMethodOfTollgateBuilder.build())
                            .addSuperinterface(ClassName.get(type_ITollgateGroup))
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Interceptor group write over. <<<");
        }
    }

    /**
     * Verify inteceptor meta
     *
     * @param element Interceptor taw type
     * @return verify result
     */
    private boolean verify(Element element) {
        Interceptor interceptor = element.getAnnotation(Interceptor.class);
        // It must be implement the interface IInterceptor and marked with annotation Interceptor.
        return null != interceptor && ((TypeElement) element).getInterfaces().contains(iInterceptor);
    }
}
