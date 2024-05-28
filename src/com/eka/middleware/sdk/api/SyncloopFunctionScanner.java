package com.eka.middleware.sdk.api;


import com.eka.middleware.sdk.api.outline.*;
import com.eka.middleware.service.ServiceUtils;
import com.sun.jdi.event.EventSet;
import org.apache.commons.lang3.StringUtils;

import javax.management.AttributeList;
import javax.management.openmbean.TabularDataSupport;
import javax.management.relation.RoleList;
import javax.management.relation.RoleUnresolvedList;
import javax.print.attribute.standard.PrinterStateReasons;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.swing.*;
import java.awt.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.security.AuthProvider;
import java.security.Provider;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.Attributes;

public class SyncloopFunctionScanner {

    public static List<ServiceOutline> addClass(Class aClass, boolean allowNonSyncloopFunctions) {

        List<ServiceOutline> serviceOutlines = new ArrayList<>();

        Constructor[] constructors = aClass.getDeclaredConstructors();

        for (Constructor constructor: constructors) {
            if (Modifier.isPrivate(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers())) {
                continue;
            }

            SyncloopFunction methodExport = (SyncloopFunction) constructor.getAnnotation(SyncloopFunction.class);

            if (null == methodExport && !allowNonSyncloopFunctions) {
                continue;
            } else if (null == methodExport) {
                methodExport = new SyncloopFunction() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }

                    @Override
                    public String title() {
                        return "";
                    }

                    @Override
                    public String description() {
                        return "";
                    }

                    @Override
                    public String[] in() {
                        Parameter[] parameters = constructor.getParameters();
                        String[] strings = new String[constructor.getParameters().length];
                        for (int i = 0; i < parameters.length ; i++) {
                            strings[i] = parameters[i].getName();
                        }
                        return strings;
                    }

                    @Override
                    public String out() {
                        return "invokingObject";
                    }

                };
            }

            String[] parametersName = methodExport.in();
            String outputParameterName = methodExport.out();
            String methodName = "new";
            String packageName = constructor.getDeclaringClass().getPackage().getName();
            boolean isStatic = Modifier.isStatic(constructor.getModifiers());
            boolean isConstructor = true;
            String className = constructor.getDeclaringClass().getName();
            Parameter[] parameters = constructor.getParameters();
            Class[] parametersTypes = new Class[parameters.length];
            String[] parametersTypesStr = new String[parameters.length];

            for (int i = 0 ; i < parameters.length ; i++) {
                parametersTypes[i] = parameters[i].getType();
                parametersTypesStr[i] = parameters[i].getType().getName();
            }

            LatestOutline latestOutline = generateInputs(methodExport, parametersName, outputParameterName, methodName,
                    aClass, isStatic, className, parameters, parametersTypesStr, isConstructor);

            List<IOOutline> output = new ArrayList<>();

            IOOutline ioOutline = new IOOutline();
            ioOutline.setText("invokingObject");
            ioOutline.setType("javaObject");
            output.add(ioOutline);

            IOOutline out = new IOOutline();
            out.setText("out");
            out.setType("document");
            out.setChildren(output);

            List<IOOutline> outlines = new ArrayList();
            outlines.add(out);
            latestOutline.setOutput(outlines);

            ServiceOutline serviceOutline = new ServiceOutline();
            serviceOutline.setLatest(latestOutline);

            serviceOutlines.add(serviceOutline);
        }

        Method[] methods = aClass.getDeclaredMethods();

        for (Method method: methods) {

            if (Modifier.isPrivate(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                continue;
            }

            SyncloopFunction methodExport = method.getAnnotation(SyncloopFunction.class);

            if (null == methodExport && !allowNonSyncloopFunctions) {
                continue;
            } else if (null == methodExport) {
                methodExport = getMethodExportRef(method);
            }

            String[] parametersName = methodExport.in();
            String outputParameterName = methodExport.out();
            String methodName = method.getName();
            Class returnType = method.getReturnType();
            String packageName = method.getDeclaringClass().getPackage().getName();
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            String className = method.getDeclaringClass().getName();
            Parameter[] parameters = method.getParameters();
            Class[] parametersTypes = new Class[parameters.length];
            String[] parametersTypesStr = new String[parameters.length];

            for (int i = 0 ; i < parameters.length ; i++) {
                parametersTypes[i] = parameters[i].getType();
                parametersTypesStr[i] = parameters[i].getType().getName();
            }

            LatestOutline latestOutline = generateInputs(methodExport, parametersName, outputParameterName, methodName,
                    returnType, isStatic, className, parameters, parametersTypesStr, false);

            String outputType = mapTypeToString(method.getGenericReturnType(), true);

            List<IOOutline> output = new ArrayList<>();

            if (StringUtils.isNotBlank(outputType)) {
                IOOutline ioOutline = new IOOutline();
                ioOutline.setText(outputParameterName);
                ioOutline.setType(outputType);
                output.add(ioOutline);

                IOOutline out = new IOOutline();
                out.setText("out");
                out.setType("document");
                out.setChildren(output);

                List<IOOutline> outlines = new ArrayList();
                outlines.add(out);
                latestOutline.setOutput(outlines);
            }

            ServiceOutline serviceOutline = new ServiceOutline();
            serviceOutline.setLatest(latestOutline);

            serviceOutlines.add(serviceOutline);
        }

        return serviceOutlines;
    }

    /**
     * @param methodExport
     * @param parametersName
     * @param outputParameterName
     * @param methodName
     * @param returnType
     * @param isStatic
     * @param className
     * @param parameters
     * @param parametersTypesStr
     * @param isConstructor
     * @return
     */
    private static LatestOutline generateInputs(SyncloopFunction methodExport, String[] parametersName,
                                                String outputParameterName, String methodName, Class returnType,
                                                boolean isStatic, String className, Parameter[] parameters,
                                                String[] parametersTypesStr, boolean isConstructor) {
        DataOutline dataOutline = new DataOutline();
        dataOutline.setAcn(className);
        dataOutline.setFunction(methodName);
        dataOutline.setArguments(parametersName);
        dataOutline.setReturnWrapper(returnType.getName());
        dataOutline.setArgumentsWrapper(parametersTypesStr);
        dataOutline.setStaticFunction(isStatic);
        dataOutline.setConstructor(isConstructor);
        dataOutline.setOutputArguments(outputParameterName);

        ApiInfoOutline apiInfoOutline = new ApiInfoOutline();
        apiInfoOutline.setTitle(methodExport.title());
        apiInfoOutline.setDescription(methodExport.description());

        LatestOutline latestOutline = new LatestOutline();
        latestOutline.setApi_info(apiInfoOutline);
        latestOutline.setData(dataOutline);

        List<IOOutline> inputs = new ArrayList<>();

        if (!isStatic && !isConstructor) {
            IOOutline invokingObject = new IOOutline();
            invokingObject.setText("invokingObject");
            invokingObject.setType("javaObject");
            inputs.add(invokingObject);
        }

        if (parameters.length > 0) {
            List<IOOutline> input = new ArrayList<>();
            for (int i = 0; i < parameters.length ; i++) {
                IOOutline ioOutline = new IOOutline();
                ioOutline.setText(parametersName[i]);
                //ioOutline.setType(parameters[i].getType().getSimpleName().toLowerCase());
                ioOutline.setType(mapTypeToString(parameters[i].getParameterizedType(), true));
                input.add(ioOutline);
            }

            IOOutline in = new IOOutline();
            in.setText("in");
            in.setType("document");
            in.setChildren(input);
            inputs.add(in);

        }
        latestOutline.setInput(inputs);
        return latestOutline;
    }

    private static SyncloopFunction getMethodExportRef(Method method) {
        SyncloopFunction methodExport;
        methodExport = new SyncloopFunction() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String title() {
                return "";
            }

            @Override
            public String description() {
                return "";
            }

            @Override
            public String[] in() {
                Parameter[] parameters = method.getParameters();
                String[] strings = new String[method.getParameters().length];
                for (int i = 0; i < parameters.length ; i++) {
                    strings[i] = parameters[i].getName();
                }
                return strings;
            }

            @Override
            public String out() {
                return "output";
            }

        };
        return methodExport;
    }

    public static ServiceOutline getContextObjectServiceViewConfig() {

        ApiInfoOutline apiInfoOutline = new ApiInfoOutline();
        apiInfoOutline.setTitle("");
        apiInfoOutline.setDescription("");

        LatestOutline latestOutline = new LatestOutline();
        latestOutline.setApi_info(apiInfoOutline);

        List<IOOutline> inputs = new ArrayList<>();
        latestOutline.setInput(inputs);

        IOOutline invokingObject = new IOOutline();
        invokingObject.setText("invokingObject");
        invokingObject.setType("javaObject");

        List<IOOutline> outlines = new ArrayList();
        outlines.add(invokingObject);
        latestOutline.setOutput(outlines);

        ServiceOutline serviceOutline = new ServiceOutline();
        serviceOutline.setLatest(latestOutline);

        return serviceOutline;
    }

    private static String mapTypeToString(Type type, boolean isSimpleType) {
        String dataType = "javaObject";
        if ((type == String.class || type == Character.class
                || type == CharSequence.class || type == char.class) && isSimpleType) {
            dataType = "string";
        } else if ((type == Integer.class || type == int.class || type == short.class) && isSimpleType) {
            dataType = "integer";
        } else if ((type == Double.class || type == Float.class || type == Long.class || type == Number.class ||
                type == double.class || type == float.class || type == long.class) && isSimpleType) {
            dataType = "number";
        } else if ((type == Void.class || type == void.class) && isSimpleType) {
            dataType = "";
        } else if (type == Date.class && isSimpleType) {
            dataType = "date";
        } else if ((type == Byte.class || type == byte.class) && isSimpleType) {
            dataType = "byte";
        } else if ((type == Boolean.class || type == boolean.class) && isSimpleType) {
            dataType = "boolean";
        } else if (MAP_CLASSES.contains(type) && isSimpleType) {
            dataType = "document";
        } else if (type instanceof Class && ((Class<?>) type).isArray()) {
            Class<?> arrayType = ((Class<?>) type).getComponentType();
            dataType = mapTypeToString(arrayType, true) + "List";
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
            if (COLLECTION_CLASSES.contains(rawType)) {
                dataType = mapTypeToString(actualTypeArgument, true) + "List";
            } else if (MAP_CLASSES.contains(rawType)) {
                dataType = mapTypeToString(rawType, true);
            }
        }
        if (dataType.contains("ListList")) {
            dataType = dataType.replaceAll("List", "") + "List";
        }
        return dataType;
    }


    private static final Set<Class> MAP_CLASSES = new HashSet<>();

    private static final Set<Class> COLLECTION_CLASSES = new HashSet();

    public static final Map<String, Class> PRIMITIVE_TYPE = new HashMap();

    static {

        MAP_CLASSES.add(AbstractMap.class);
        MAP_CLASSES.add(Attributes.class );
        MAP_CLASSES.add(AuthProvider.class);
        MAP_CLASSES.add(ConcurrentHashMap.class);
        MAP_CLASSES.add(ConcurrentSkipListMap.class );
        MAP_CLASSES.add(Map.class);
        MAP_CLASSES.add(EnumMap.class);
        MAP_CLASSES.add(HashMap.class);
        MAP_CLASSES.add(com.eka.middleware.heap.HashMap.class);
        MAP_CLASSES.add(Hashtable.class );
        MAP_CLASSES.add(IdentityHashMap.class );
        MAP_CLASSES.add(LinkedHashMap.class);
        MAP_CLASSES.add(PrinterStateReasons.class );
        MAP_CLASSES.add(Properties.class );
        MAP_CLASSES.add(Provider.class);
        MAP_CLASSES.add(RenderingHints.class );
        MAP_CLASSES.add(SimpleBindings.class );
        MAP_CLASSES.add(TabularDataSupport.class);
        MAP_CLASSES.add(TreeMap.class );
        MAP_CLASSES.add(UIDefaults.class);
        MAP_CLASSES.add(WeakHashMap.class);
        MAP_CLASSES.add(Bindings.class );
        MAP_CLASSES.add(ConcurrentMap.class );
        MAP_CLASSES.add(ConcurrentNavigableMap.class );
        MAP_CLASSES.add(NavigableMap.class );
        MAP_CLASSES.add(SortedMap.class);

        COLLECTION_CLASSES.add(BlockingDeque.class);
        COLLECTION_CLASSES.add(BlockingQueue.class);
        COLLECTION_CLASSES.add(Deque.class);
        COLLECTION_CLASSES.add(EventSet.class );
        COLLECTION_CLASSES.add(List.class );
        COLLECTION_CLASSES.add(NavigableSet.class );
        COLLECTION_CLASSES.add(Queue.class );
        COLLECTION_CLASSES.add(Set.class);
        COLLECTION_CLASSES.add(SortedSet.class );
        COLLECTION_CLASSES.add(TransferQueue.class);
        COLLECTION_CLASSES.add(AbstractCollection.class);
        COLLECTION_CLASSES.add(AbstractList.class );
        COLLECTION_CLASSES.add(AbstractQueue.class );
        COLLECTION_CLASSES.add(AbstractSequentialList.class );
        COLLECTION_CLASSES.add(AbstractSet.class);
        COLLECTION_CLASSES.add(ArrayBlockingQueue.class );
        COLLECTION_CLASSES.add(ArrayDeque.class );
        COLLECTION_CLASSES.add(ArrayList.class );
        COLLECTION_CLASSES.add(AttributeList.class);
        COLLECTION_CLASSES.add(ConcurrentHashMap.KeySetView.class);
        COLLECTION_CLASSES.add(ConcurrentLinkedDeque.class );
        COLLECTION_CLASSES.add(ConcurrentLinkedQueue.class );
        COLLECTION_CLASSES.add(ConcurrentSkipListSet.class );
        COLLECTION_CLASSES.add(CopyOnWriteArrayList.class );
        COLLECTION_CLASSES.add(CopyOnWriteArraySet.class);
        COLLECTION_CLASSES.add(DelayQueue.class );
        COLLECTION_CLASSES.add(EnumSet.class );
        COLLECTION_CLASSES.add(HashSet.class );
        COLLECTION_CLASSES.add(LinkedBlockingDeque.class);
        COLLECTION_CLASSES.add(LinkedBlockingQueue.class );
        COLLECTION_CLASSES.add(LinkedHashSet.class );
        COLLECTION_CLASSES.add(LinkedList.class );
        COLLECTION_CLASSES.add(LinkedTransferQueue.class);
        COLLECTION_CLASSES.add(PriorityBlockingQueue.class);
        COLLECTION_CLASSES.add(PriorityQueue.class );
        COLLECTION_CLASSES.add(RoleList.class);
        COLLECTION_CLASSES.add(RoleUnresolvedList.class);
        COLLECTION_CLASSES.add(Stack.class );
        COLLECTION_CLASSES.add(SynchronousQueue.class );
        COLLECTION_CLASSES.add(TreeSet.class );
        COLLECTION_CLASSES.add(Vector.class);

        PRIMITIVE_TYPE.put("int", int.class);
        PRIMITIVE_TYPE.put("short", short.class);
        PRIMITIVE_TYPE.put("void", void.class);
        PRIMITIVE_TYPE.put("double", double.class);
        PRIMITIVE_TYPE.put("float", float.class);
        PRIMITIVE_TYPE.put("long", long.class);
        PRIMITIVE_TYPE.put("boolean", boolean.class);
        PRIMITIVE_TYPE.put("char", char.class);
    }
}