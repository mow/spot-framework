package at.spot.core.infrastructure.aspect;

//@Aspect
public class ItemPropertyAccessAspect extends AbstractBaseAspect {
	//
	// @Autowired
	// protected ModelService modelService;
	//
	// @Autowired
	// protected QueryService queryService;
	//
	// // @Autowired
	// Map<String, ItemPropertyValueProvider> itemPropertyValueProviders = new
	// HashMap<>();
	//
	// /*
	// * PointCuts
	// */
	//
	// @Pointcut("!within(at.spot.core.persistence..*) &&
	// !within(at.spot.core.infrastructure.aspect..*) &&
	// !within(at.spot.core.model..*)")
	// protected void notFromPersistencePackage() {
	// };
	//
	// /**
	// * Define the pointcut for all fields that are accessed (get) on an object
	// * of type @Item that are annotated with @Property.
	// */
	// @Pointcut("@annotation(at.spot.core.infrastructure.annotation.Property)
	// && get(* *.*)")
	// final protected void getField() {
	// };
	//
	// /**
	// * Define the pointcut for all getter that are accessing a field in an
	// * object of type @Item that are annotated with @Property.
	// */
	// @Pointcut("@annotation(at.spot.core.infrastructure.annotation.GetProperty)
	// && within(@at.spot.core.infrastructure.annotation.ItemType *)")
	// final protected void getMethod() {
	// };
	//
	// /**
	// * Define the pointcut for all fields that are accessed (set) on an object
	// * of type @Item that are annotated with @Property.
	// */
	// @Pointcut("@annotation(at.spot.core.infrastructure.annotation.Property)
	// && set(* *.*)")
	// final protected void setField() {
	// };
	//
	// /**
	// * Define the pointcut for all fields that are accessed (set) on an object
	// * of type @Item that are annotated with @Property.
	// */
	// @Pointcut("@annotation(at.spot.core.infrastructure.annotation.SetProperty)
	// && "
	// + "within(@at.spot.core.infrastructure.annotation.ItemType *) &&
	// execution(* set*(..))")
	// final protected void setMethod() {
	// };
	//
	// /*
	// * JoinPoints
	// */
	//
	// @After("(setField() || setMethod()) && notFromPersistencePackage()")
	// public void setPropertyValue(final JoinPoint joinPoint) {
	// final Property ann = getAnnotation(joinPoint, Property.class);
	//
	// if (ann != null && !ann.writable()) {
	// throw new RuntimeException(String.format("Attribute %s is not writable.",
	// createSignature(joinPoint)));
	// }
	//
	// // handle relation annotation
	// final Relation rel = getAnnotation(joinPoint, Relation.class);
	//
	// if (rel != null) {
	// loggingService.warn("Handling relations not implemented here.");
	// // handleRelationProperty(joinPoint, rel);
	// }
	//
	// // set the changed field to dirty
	// if (joinPoint.getTarget() instanceof Item) {
	// ClassUtil.invokeMethod(joinPoint.getTarget(), "markAsDirty",
	// joinPoint.getSignature().getName());
	// }
	// }
	//
	// @Around("(getField() || getMethod()) && notFromPersistencePackage()")
	// public Object getPropertyValue(final ProceedingJoinPoint joinPoint)
	// throws Throwable {
	// final Property ann = getAnnotation(joinPoint, Property.class);
	//
	// if (ann == null || !ann.readable()) {
	// throw new RuntimeException(String.format("Attribute %s is not readable.",
	// createSignature(joinPoint)));
	// }
	//
	// // if the target is a proxy item, we load it first, then we invoke the
	// // getter functionality
	// if (joinPoint.getTarget() instanceof Item) {
	// final Item i = (Item) joinPoint.getTarget();
	//
	// if (i.isProxy) {
	// modelService.loadProxyModel(i);
	// }
	// }
	//
	// // if there's a value provider configured, use it
	// if (StringUtils.isNotBlank(ann.itemValueProvider())) {
	// final ItemPropertyValueProvider pv =
	// itemPropertyValueProviders.get(ann.itemValueProvider());
	// return pv.readValue((Item) joinPoint.getTarget(),
	// joinPoint.getSignature().getName());
	// } else { // get currently stored object
	// final Object retVal = getPropertyValueInternal(joinPoint);
	// return retVal;
	// }
	// }
	//
	// protected Object getPropertyValueInternal(final ProceedingJoinPoint
	// joinPoint) throws Throwable {
	// return joinPoint.proceed(joinPoint.getArgs());
	// }
}
