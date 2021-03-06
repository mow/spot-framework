package io.spotnext.infrastructure.instrumentation;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.spotnext.infrastructure.annotation.ItemType;
import io.spotnext.infrastructure.annotation.Property;
import io.spotnext.infrastructure.annotation.Relation;
import io.spotnext.infrastructure.maven.xml.DatabaseColumnType;
import io.spotnext.infrastructure.type.Item;
import io.spotnext.infrastructure.type.RelationNodeType;
import io.spotnext.infrastructure.type.RelationType;
import io.spotnext.support.weaving.AbstractBaseClassTransformer;
import io.spotnext.support.weaving.IllegalClassTransformationException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * Transforms custom {@link ItemType} annotations to JPA entity annotations.
 */
public class JpaEntityClassTransformer extends AbstractBaseClassTransformer {

	private static final Logger LOG = LoggerFactory.getLogger(JpaEntityClassTransformer.class);

	protected static final String MV_CASCADE = "cascade";
	protected static final String MV_NODE_TYPE = "nodeType";
	protected static final String MV_REFERENCED_COLUMN_NAME = "referencedColumnName";
	protected static final String MV_ID = "id";
	protected static final String MV_INVERSE_JOIN_COLUMNS = "inverseJoinColumns";
	protected static final String MV_JOIN_COLUMNS = "joinColumns";
	protected static final String MV_NAME = "name";
	protected static final String MV_RELATION_NAME = "relationName";
	protected static final String MV_PERSISTABLE = "persistable";
	protected static final String CLASS_FILE_SUFFIX = ".class";
	protected static final String MV_MAPPED_BY = "mappedBy";
	protected static final String MV_MAPPED_TO = "mappedTo";
	protected static final String MV_REFERENCED_TYPE = "referencedType";
	protected static final String MV_TYPE = "type";
	protected static final String MV_TYPE_CODE = "typeCode";
	protected static final String MV_VALUE = "value";
	protected static final String MV_UNIQUE = "unique";
	protected static final String MV_INDEXED = "indexed";
	protected static final String MV_NULLABLE = "nullable";
	protected static final String MV_COLUMN_NAMES = "columnNames";
	protected static final String MV_UNIQUE_CONSTRAINTS = "uniqueConstraints";
	protected static final String MV_COLUMN_TYPE = "columnType";
	protected static final String RELATION_SOURCE_COLUMN = "source_id";
	protected static final String RELATION_TARGET_COLUMN = "target_id";

	@SuppressFBWarnings({ "REC_CATCH_EXCEPTION" })
	@Override
	protected Optional<CtClass> transform(final ClassLoader loader, final CtClass clazz,
			final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain)
			throws IllegalClassTransformationException {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Transforming: " + clazz.getName());
		}

		try {
			// we only want to transform item types only ...
			if (isItemType(clazz) && !alreadyTransformed(clazz)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Injecting JPA annotations: " + clazz.getName());
				}

				if (clazz.isFrozen()) {
					try {
						clazz.defrost();
					} catch (final Exception e) {
						throw new IllegalClassTransformationException(
								String.format("Type %s was frozen and could not be defrosted", clazz.getName()), e);
					}
				}

				// add JPA entity annotation
				addEntityAnnotation(clazz);

				// process item properties
				for (final CtField field : getDeclaredFields(clazz)) {
					if (!clazz.equals(field.getDeclaringClass())) {
						continue;
					}

					final Optional<Annotation> propertyAnn = getAnnotation(field, Property.class);

					// process item type property annotation
					if (propertyAnn.isPresent()) {
						// create the necessary JPA annotations based on
						// Relation and Property annotations
						final List<Annotation> fieldAnnotations = createJpaRelationAnnotations(clazz, field,
								propertyAnn.get());

						{ // mark property as unique and add indexes
							final Optional<Annotation> uniqueAnn = createUniqueConstraintAnnotation(field,
									propertyAnn.get());
							final Optional<Annotation> indexAnn = createIndexAnnotation(clazz, field, propertyAnn.get());

							if (uniqueAnn.isPresent()) {
								fieldAnnotations.add(uniqueAnn.get());
							}

							if (indexAnn.isPresent()) {
								fieldAnnotations.add(indexAnn.get());
							}
						}

						// only add column annotation if there is no relation
						// annotation, as this is not
						// allowed
						if (CollectionUtils.isEmpty(fieldAnnotations)) {
							final List<Annotation> columnAnn = createColumnAnnotation(clazz, field, propertyAnn.get());
							fieldAnnotations.addAll(columnAnn);
						}

						// and add them to the clazz
						addAnnotations(clazz, field, fieldAnnotations);
					}
				}

//				addIndexAnnotation(clazz);

				return Optional.of(clazz);
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Ignoring " + clazz.getName());
				}
			}
		} catch (final Exception e) {
			logException(e);

			throw new IllegalClassTransformationException(
					String.format("Unable to process JPA annotations for class file %s, reason: %s", clazz.getName(), e.getMessage()), e);
		}

		return Optional.empty();
	}

	protected boolean alreadyTransformed(final CtClass clazz) throws IllegalClassTransformationException {
		final Optional<Annotation> entityAnnotation = getAnnotation(clazz, Entity.class);
		final Optional<Annotation> mappedSuperclassAnnotation = getAnnotation(clazz, MappedSuperclass.class);

		return entityAnnotation.isPresent() || mappedSuperclassAnnotation.isPresent();
	}

	protected void addEntityAnnotation(final CtClass clazz) throws IllegalClassTransformationException {
		final Optional<Annotation> itemTypeAnn = getItemTypeAnnotation(clazz);

		if (itemTypeAnn.isPresent()) {
			final BooleanMemberValue val = (BooleanMemberValue) itemTypeAnn.get().getMemberValue(MV_PERSISTABLE);

			if (val != null && val.getValue()) {
				// this type needs a separate deployment table
				addAnnotations(clazz, Arrays.asList(createAnnotation(clazz, Entity.class)));
			} else {
				// this type is not an persistable entity
				addAnnotations(clazz, Arrays.asList(createAnnotation(clazz, MappedSuperclass.class)));
			}

			// enables dynamic update for this entity, only writes changed fields
//			addAnnotations(clazz, Arrays.asList(createAnnotation(clazz, DynamicUpdate.class)));
		}
	}

	protected boolean isItemType(final CtClass clazz) throws IllegalClassTransformationException {
		return getItemTypeAnnotation(clazz).isPresent();
	}

	protected Optional<Annotation> getItemTypeAnnotation(final CtClass clazz)
			throws IllegalClassTransformationException {

		// if the given class is a java base class it can't be unfrozen and it
		// would
		// throw an exception so we check for valid classes
		if (isValidClass(clazz.getName())) {
			return getAnnotation(clazz, ItemType.class);
		}

		return Optional.empty();
	}

	protected String getItemTypeCode(final CtClass clazz) throws IllegalClassTransformationException {
		final Optional<Annotation> ann = getItemTypeAnnotation(clazz);

		if (ann.isPresent()) {
			final StringMemberValue typeCode = (StringMemberValue) ann.get().getMemberValue(MV_TYPE_CODE);

			return typeCode.getValue();
		}

		return null;
	}

	protected List<Annotation> createColumnAnnotation(final CtClass clazz, final CtField field,
			final Annotation propertyAnnotation) {

		final List<Annotation> ret = new ArrayList<>();

		final Annotation columnAnn = createAnnotation(field.getFieldInfo2().getConstPool(), Column.class);

		final StringMemberValue columnName = new StringMemberValue(field.getFieldInfo2().getConstPool());
		columnName.setValue(field.getName());
		columnAnn.addMemberValue("name", columnName);

		if (isUniqueProperty(field, propertyAnnotation)) {
			final BooleanMemberValue nullable = new BooleanMemberValue(field.getFieldInfo2().getConstPool());
			nullable.setValue(false);
			columnAnn.addMemberValue("nullable", nullable);
		}

		ret.add(columnAnn);

		// add the type information, if available

		final EnumMemberValue colTypeVal = (EnumMemberValue) propertyAnnotation.getMemberValue(MV_COLUMN_TYPE);

		if (colTypeVal != null) {
			DatabaseColumnType columnTypeEnumVal;
			try {
				columnTypeEnumVal = DatabaseColumnType.valueOf(colTypeVal.getValue());
			} catch (final Exception e) {
				columnTypeEnumVal = DatabaseColumnType.DEFAULT;
			}

			if (!DatabaseColumnType.DEFAULT.equals(columnTypeEnumVal)) {
				final Annotation typeAnn = createAnnotation(field.getFieldInfo2().getConstPool(), Type.class);

				final StringMemberValue typeName = new StringMemberValue(field.getFieldInfo2().getConstPool());
				typeName.setValue(mapColumnTypeToHibernate(columnTypeEnumVal));
				typeAnn.addMemberValue("type", typeName);

				ret.add(typeAnn);
			}
		}

		return ret;
	}

	/**
	 * Checks if the field has the {@link Property#unique()} annotation value set. If yes, then a {@link NotNull} is added. The real uniqueness constraint is
	 * checked using {@link Item#uniquenessHash()}.
	 *
	 * @param field              for which the annotation will be created
	 * @param propertyAnnotation the property annotation that holds information about the item type property.
	 * @return the created annotation
	 */
	protected Optional<Annotation> createUniqueConstraintAnnotation(final CtField field, final Annotation propertyAnnotation) {

		Annotation ann = null;

		if (isUniqueProperty(field, propertyAnnotation)) {
			ann = createAnnotation(field.getFieldInfo2().getConstPool(), NotNull.class);
		}

		return Optional.ofNullable(ann);
	}

	// @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
	protected Optional<Annotation> createIndexAnnotation(final CtClass itemType, final CtField field, final Annotation propertyAnnotation) {
		Annotation ann = null;

		if (isIndexable(field, propertyAnnotation) && !getAnnotation(field, org.hibernate.annotations.Index.class).isPresent()) {
			ann = createAnnotation(field.getFieldInfo2().getConstPool(), org.hibernate.annotations.Index.class);

			final StringMemberValue indexName = new StringMemberValue(field.getFieldInfo2().getConstPool());
			indexName.setValue("idx_" + itemType.getSimpleName() + "_" + field.getName());
			ann.addMemberValue("name", indexName);
		}

		return Optional.ofNullable(ann);
	}

	// //@SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
	protected void addIndexAnnotation(CtClass itemType) throws IllegalClassTransformationException {
		Annotation tableAnnotation = getAnnotation(itemType, Table.class).orElse(null);

		if (tableAnnotation == null) {
			tableAnnotation = createAnnotation(getConstPool(itemType), Table.class);
		}

		final List<AnnotationMemberValue> indexAnnotations = new ArrayList<>();

		for (CtField field : itemType.getFields()) {
			Optional<Annotation> propertyAnn = getAnnotation(field, Property.class);

			if (propertyAnn.isPresent() && isIndexable(field, propertyAnn.get())) {

				Annotation indexAnnotation = createAnnotation(getConstPool(itemType), Index.class);

				StringMemberValue indexName = new StringMemberValue(getConstPool(itemType));
				indexName.setValue("idx_" + field.getName());
				indexAnnotation.addMemberValue("name", indexName);

				StringMemberValue columnList = new StringMemberValue(getConstPool(itemType));
				columnList.setValue(field.getName());

				indexAnnotation.addMemberValue("columnList", columnList);

				final AnnotationMemberValue v = new AnnotationMemberValue(getConstPool(itemType));
				v.setValue(indexAnnotation);

				indexAnnotations.add(v);
			}
		}

		final ArrayMemberValue tableIndexesValue = new ArrayMemberValue(getConstPool(itemType));
		tableIndexesValue.setValue(indexAnnotations.toArray(new MemberValue[indexAnnotations.size()]));

		tableAnnotation.addMemberValue("indexes", tableIndexesValue);

		addAnnotations(itemType, Arrays.asList(tableAnnotation));
	}

	/**
	 * Checks if {@link Property#unique()} = true.
	 *
	 * @param field              the field which should be checked for a uniqueness constraint
	 * @param propertyAnnotation annotation containing item type property information
	 * @return true if the property has a unique constraint
	 */
	protected boolean isUniqueProperty(final CtField field, final Annotation propertyAnnotation) {
		final BooleanMemberValue val = (BooleanMemberValue) propertyAnnotation.getMemberValue(MV_UNIQUE);
		return val != null ? val.getValue() : false;
	}

	protected boolean isIndexable(final CtField field, final Annotation propertyAnnotation) {
		final BooleanMemberValue val = (BooleanMemberValue) propertyAnnotation.getMemberValue(MV_INDEXED);
		return val != null ? val.getValue() : false;
	}

	protected List<Annotation> createJpaRelationAnnotations(final CtClass entityClass, final CtField field,
			final Annotation propertyAnnotation) throws NotFoundException, IllegalClassTransformationException {

		final List<Annotation> jpaAnnotations = new ArrayList<>();

		final Optional<Annotation> relAnnotation = getAnnotation(field, Relation.class);

		jpaAnnotations.add(createCacheAnnotation(field));

		if (relAnnotation.isPresent()) {
			final EnumMemberValue relType = (EnumMemberValue) relAnnotation.get().getMemberValue(MV_TYPE);

			// JPA Relation annotations
			if (StringUtils.equals(relType.getValue(), RelationType.ManyToMany.toString())) {
				jpaAnnotations.addAll(createCascadeAnnotations(entityClass, field, ManyToMany.class, null, propertyAnnotation));

				// necessary for serialization
				jpaAnnotations.addAll(createSerializationAnnotation(entityClass, field, true));

				// necessary for FETCH JOINS
				jpaAnnotations.addAll(createOrderedListAnnotation(entityClass, field));

				// JoinTable annotation for bi-directional m-to-n relation table
				jpaAnnotations
						.add(createJoinTableAnnotation(entityClass, field, propertyAnnotation, relAnnotation.get()));

			} else if (StringUtils.equals(relType.getValue(), RelationType.OneToMany.toString())) {
				final List<Annotation> o2mAnn = createCascadeAnnotations(entityClass, field, OneToMany.class,
						relAnnotation.get(), propertyAnnotation);
				jpaAnnotations.addAll(o2mAnn);

				// necessary for serialization
				jpaAnnotations.addAll(createSerializationAnnotation(entityClass, field, true));
				// jpaAnnotations.add(createCollectionTypeAnnotation(entityClass, field));

				// necessary for FETCH JOINS
				jpaAnnotations.addAll(createOrderedListAnnotation(entityClass, field));

			} else if (StringUtils.equals(relType.getValue(), RelationType.ManyToOne.toString())) {
				jpaAnnotations.addAll(createCascadeAnnotations(entityClass, field, ManyToOne.class, null, propertyAnnotation));
				jpaAnnotations.add(createJoinColumnAnnotation(entityClass, field));

				// necessary for serialization
				jpaAnnotations.addAll(createSerializationAnnotation(entityClass, field, false));
			} else {
				// one to one in case the field type is a subtype of Item
				jpaAnnotations.addAll(createCascadeAnnotations(entityClass, field, OneToOne.class, null, propertyAnnotation));
			}

		} else if (isItemType(field.getType())) {
			// one to one in case the field type is a subtype of Item
			jpaAnnotations.addAll(createCascadeAnnotations(entityClass, field, ManyToOne.class, null, propertyAnnotation));
			jpaAnnotations.add(createJoinColumnAnnotation(entityClass, field));

			// necessary for serialization
			jpaAnnotations.addAll(createSerializationAnnotation(entityClass, field, false));
		} else if (hasInterface(field.getType(), Collection.class) || hasInterface(field.getType(), Map.class)) {
			jpaAnnotations.addAll(createElementCollectionAnnotation(entityClass, field));

			// necessary for serialization
			jpaAnnotations.addAll(createSerializationAnnotation(entityClass, field, true));
		}

		return jpaAnnotations;
	}

	/**
	 * Adds a {@link Cache} annotation to every relation property. This is necessary to enable hibernate's second level cache for the relation the value(s).
	 *
	 * @param field the field to apply the annotation on
	 * @return the created cache annotation
	 */
	private Annotation createCacheAnnotation(CtField field) {
		final Annotation ann = createAnnotation(field.getFieldInfo2().getConstPool(), Cache.class);

		final EnumMemberValue usage = new EnumMemberValue(field.getFieldInfo2().getConstPool());
		usage.setType(CacheConcurrencyStrategy.class.getName());
		usage.setValue(CacheConcurrencyStrategy.NONSTRICT_READ_WRITE.name());

		final StringMemberValue region = new StringMemberValue(field.getFieldInfo2().getConstPool());
		region.setValue("items");

		ann.addMemberValue("usage", usage);
		ann.addMemberValue("region", region);

		return ann;
	}

	/**
	 * Annotates relation collections with an {@link OrderBy} annotation to make FETCH JOINS work correctly.
	 *
	 * @param entityClass for which the annotations are created
	 * @param field       for which the annotations are created
	 * @return list of created annotations, never null
	 * @throws IllegalClassTransformationException in case there is an error accessing class or field internals
	 */
	protected List<Annotation> createOrderedListAnnotation(final CtClass entityClass, final CtField field)
			throws IllegalClassTransformationException {

		final List<Annotation> annotations = new ArrayList<>();

		return annotations;
	}

	private Optional<CtMethod> getGetter(CtClass entityClass, CtField field) {
		return getMethod(entityClass, "get" + StringUtils.capitalize(field.getName()));
	}

	private Optional<CtMethod> getSetter(CtClass entityClass, CtField field) {
		return getMethod(entityClass, "set" + StringUtils.capitalize(field.getName()));
	}

	/**
	 * Finds the method with the given name, ignoring any method parameters.
	 * 
	 * @param entityClass the class to inspect
	 * @param methodName
	 * @return the method or null
	 */
	protected Optional<CtMethod> getMethod(CtClass entityClass, String methodName) {
		CtMethod method = null;

		try {
			method = entityClass.getDeclaredMethod(methodName);
		} catch (NotFoundException e) {
			// ignore
		}

		return Optional.ofNullable(method);
	}

	/**
	 * Necessary to prohibit infinite loops when serializing using Jackson
	 *
	 * @param entityClass  the class of the field
	 * @param field        the field which will be annotated with a JsonSerialize annotation.
	 * @param isCollection defines if the collection de/serializer annotation should be applied, or not
	 * @return the created annotation, never null
	 * @throws IllegalClassTransformationException in case there is an error accessing class or field internals
	 * @throws NotFoundException
	 */
	protected List<Annotation> createSerializationAnnotation(final CtClass entityClass, final CtField field,
			final boolean isCollection) throws IllegalClassTransformationException, NotFoundException {

		List<Annotation> ret = new ArrayList<>();

		final Annotation jsonSerializeAnn = createAnnotation(entityClass, JsonSerialize.class);

		// serializer
		final ClassMemberValue serVal = new ClassMemberValue(field.getFieldInfo2().getConstPool());
		serVal.setValue(isCollection //
				? "io.spotnext.core.infrastructure.serialization.jackson.ItemCollectionProxySerializer" //
				: "io.spotnext.core.infrastructure.serialization.jackson.ItemProxySerializer");
		jsonSerializeAnn.addMemberValue("using", serVal);
		ret.add(jsonSerializeAnn);

		if (isCollection) {
//			// ignore field as we have to put the deserializer annotation for collections on the getter
//			final Annotation jsonPropertyAnn = createAnnotation(entityClass, JsonProperty.class);
//			EnumMemberValue accessVal = new EnumMemberValue(field.getFieldInfo2().getConstPool());
//			accessVal.setType(JsonProperty.Access.class.getName());
//			accessVal.setValue("READ_ONLY");
//			jsonPropertyAnn.addMemberValue("access", accessVal);
//			ret.add(jsonPropertyAnn);
//
//			// add actual deserializer annotation to getter
//			final Optional<CtMethod> getter = getGetter(entityClass, field);
//			if (getter.isPresent()) {
//				final Annotation jsonDeserializeAnn = createAnnotation(entityClass, JsonDeserialize.class);
//				final ClassMemberValue deserVal = new ClassMemberValue(field.getFieldInfo2().getConstPool());
//				deserVal.setValue("io.spotnext.core.infrastructure.serialization.jackson.ItemCollectionDeserializer");
//				jsonDeserializeAnn.addMemberValue("using", deserVal);
//
//				final Annotation getterJsonProperty = createAnnotation(entityClass, JsonProperty.class);
//
//				addAnnotations(getter.get(), Arrays.asList(jsonDeserializeAnn, getterJsonProperty));
//			}
//
//			final Optional<CtMethod> setter = getSetter(entityClass, field);
//			if (setter.isPresent()) {
//				final Annotation jsonIgnoreAnn = createAnnotation(entityClass, JsonIgnore.class);
//				addAnnotations(setter.get(), Arrays.asList(jsonIgnoreAnn));
//			}

			// configure the Jackson XML collection name
			final Annotation xmlCollectionAnn = createAnnotation(getConstPool(entityClass),
					"javax.xml.bind.annotation.XmlElementWrapper");

			final StringMemberValue collectionName = new StringMemberValue(field.getFieldInfo2().getConstPool());
			collectionName.setValue(field.getName());
			xmlCollectionAnn.addMemberValue("name", collectionName);
			ret.add(xmlCollectionAnn);

			// configure the Jackson XML collection element name
			final Annotation xmlCollectionElementAnn = createAnnotation(getConstPool(entityClass),
					"javax.xml.bind.annotation.XmlElement");

			final StringMemberValue elementName = new StringMemberValue(field.getFieldInfo2().getConstPool());
			elementName.setValue(field.getName() + "Entry");
			xmlCollectionElementAnn.addMemberValue("name", elementName);
			ret.add(xmlCollectionElementAnn);

			// define json alias so that the collection can be deserialized correctly
			final Annotation jsonAliasAnn = createAnnotation(getConstPool(entityClass),
					"com.fasterxml.jackson.annotation.JsonAlias");

			final StringMemberValue alias = new StringMemberValue(field.getFieldInfo2().getConstPool());
			alias.setValue(field.getName());

			ArrayMemberValue aliases = new ArrayMemberValue(field.getFieldInfo2().getConstPool());
			aliases.setValue(new MemberValue[] { alias });
			jsonAliasAnn.addMemberValue("value", aliases);

			ret.add(jsonAliasAnn);
		}

		return ret;
	}

	protected void addMappedByAnnotationValue(final CtField field, final Annotation annotation,
			final CtClass entityClass, final Annotation relation) {

		if (relation != null) {
			final StringMemberValue mappedTo = (StringMemberValue) relation.getMemberValue(MV_MAPPED_TO);

			if (mappedTo != null && StringUtils.isNotBlank(mappedTo.getValue())) {
				annotation.addMemberValue(MV_MAPPED_BY,
						createAnnotationStringValue(field.getFieldInfo2().getConstPool(), mappedTo.getValue()));
			}
		}
	}

	protected List<Annotation> createElementCollectionAnnotation(final CtClass clazz, final CtField field)
			throws IllegalClassTransformationException {

		final List<Annotation> ret = new ArrayList<>();

		{ // ElementCollection
			final Annotation ann = createAnnotation(clazz, ElementCollection.class);
//			addJpaCascadeAnnotation(ann, field);

			// add fetch type
			final EnumMemberValue fetchType = new EnumMemberValue(getConstPool(clazz));
			fetchType.setType(FetchType.class.getName());
			fetchType.setValue(FetchType.LAZY.name());
			ann.addMemberValue("fetch", fetchType);
			ret.add(ann);
		}

		{ // CollectionTable
			final Annotation ann = createAnnotation(clazz, CollectionTable.class);
//			addJpaCascadeAnnotation(ann, field);

			// add fetch type
			final StringMemberValue tableName = new StringMemberValue(getConstPool(clazz));
			tableName.setValue(clazz.getSimpleName() + "_" + field.getName());
			ann.addMemberValue("name", tableName);
			ret.add(ann);
		}

		return ret;
	}

	protected List<Annotation> createCascadeAnnotations(final CtClass clazz, final CtField field,
			final Class<? extends java.lang.annotation.Annotation> annotationType, Annotation relationAnnotation, Annotation propertyAnnotation)
			throws IllegalClassTransformationException, NotFoundException {

		final List<Annotation> annotations = new ArrayList<>();

		boolean addRemoveCascadeType = false;

		if (OneToMany.class.equals(annotationType)) {
			final StringMemberValue mappedToVal = (StringMemberValue) relationAnnotation.getMemberValue(MV_MAPPED_TO);
			final ClassMemberValue referencedTypeVal = (ClassMemberValue) relationAnnotation.getMemberValue(MV_REFERENCED_TYPE);

			if (mappedToVal != null && StringUtils.isNotBlank(mappedToVal.getValue()) && referencedTypeVal != null) {
				CtClass referencedType = classPool.get(referencedTypeVal.getValue());
				final CtField otherField = referencedType.getField(mappedToVal.getValue());
				final Optional<Annotation> otherPropertyAnnotation = getAnnotation(otherField, Property.class);

				if (otherPropertyAnnotation.isPresent()) {
					boolean isUnique = otherPropertyAnnotation //
							.map(a -> a.getMemberValue(MV_UNIQUE)) //
							.filter(m -> m instanceof BooleanMemberValue) //
							.map(v -> ((BooleanMemberValue) v).getValue()) //
							.orElse(false);

					addRemoveCascadeType = isUnique;
				}
			}
		}

		final Annotation x2xAnn = addJpaCascadeAnnotation(field, annotationType, addRemoveCascadeType);

		annotations.add(x2xAnn);
		annotations.add(addHibernateCascadeAnnotation(field, addRemoveCascadeType));

		if (relationAnnotation != null) {
			addMappedByAnnotationValue(field, x2xAnn, clazz, relationAnnotation);
		}

		return annotations;
	}

	/**
	 * @param clazz the class of the given field
	 * @param field the field for which the annotation is created
	 * @return the created collection type annotation
	 * @throws IllegalClassTransformationException in case there is an error accessing class or field internals
	 */
	protected Annotation createCollectionTypeAnnotation(final CtClass clazz, final CtField field)
			throws IllegalClassTransformationException {

		final Annotation ann = createAnnotation(clazz, CollectionType.class);

		final Optional<Annotation> relationCollectionType = getAnnotation(field, Relation.class);

		if (relationCollectionType.isPresent()) {
//            final EnumMemberValue val = (EnumMemberValue) relationCollectionType.get().getMemberValue("collectionType");

			final StringMemberValue typeVal = new StringMemberValue(field.getFieldInfo2().getConstPool());

//            if (val != null && RelationCollectionType.Set.toString().equals(val.getValue())) {
//                typeVal.setValue("io.spotnext.core.persistence.hibernate.support.usertypes.RelationshipMaintainingSetType");
//            } else {
//                typeVal.setValue("io.spotnext.core.persistence.hibernate.support.usertypes.RelationshipMaintainingListType");
//            }

			// use set types for now, as hibernate has issues with lists and fetch joins
			typeVal.setValue("io.spotnext.core.persistence.hibernate.support.usertypes.RelationshipMaintainingSetType");

			ann.addMemberValue("type", typeVal);
		}

		return ann;
	}

	/**
	 * Creates a {@link JoinColumn} annotation annotation in case the property has a unique=true modifier.
	 *
	 * @param clazz the class containing the field
	 * @param field for which the annotation will be created
	 * @return the created annotation
	 * @throws IllegalClassTransformationException in case there is an error accessing class or field internals
	 */
	protected Annotation createJoinColumnAnnotation(final CtClass clazz, final CtField field)
			throws IllegalClassTransformationException {

		final Annotation ann = createAnnotation(clazz, JoinColumn.class);

		final Optional<Annotation> propAnnotation = getAnnotation(field, Property.class);

		if (propAnnotation.isPresent()) {
			final BooleanMemberValue uniqueVal = (BooleanMemberValue) propAnnotation.get().getMemberValue("unique");

			if (uniqueVal != null && uniqueVal.getValue()) {
				// unique value
				// final BooleanMemberValue unique = new
				// BooleanMemberValue(field.getFieldInfo2().getConstPool());
				// unique.setValue(true);
				//
				// ann.addMemberValue(MV_UNIQUE, unique);

				// nullable value
				final BooleanMemberValue nullable = new BooleanMemberValue(field.getFieldInfo2().getConstPool());
				nullable.setValue(false);

				ann.addMemberValue(MV_NULLABLE, nullable);
			}
		}

		// column name
		final StringMemberValue columnName = new StringMemberValue(field.getFieldInfo2().getConstPool());
		columnName.setValue(field.getName() + "_id");

		ann.addMemberValue(MV_NAME, columnName);

		return ann;
	}

	protected Annotation createJoinTableAnnotation(final CtClass clazz, final CtField field,
			final Annotation propertyAnnotation, final Annotation relationAnnotation) {

		final StringMemberValue relationNameValue = (StringMemberValue) relationAnnotation
				.getMemberValue(MV_RELATION_NAME);

		// @JoinTable
		final Annotation joinTableAnn = createAnnotation(field.getFieldInfo2().getConstPool(), JoinTable.class);
		final StringMemberValue tableName = new StringMemberValue(field.getFieldInfo2().getConstPool());

		// generate relation table name
		tableName.setValue(relationNameValue.getValue());
		joinTableAnn.addMemberValue(MV_NAME, tableName);

		{// swap relationnode types according to the relation setting
			String joinColumnName = RELATION_SOURCE_COLUMN;
			String inverseJoinColumnName = RELATION_TARGET_COLUMN;

			final RelationNodeType nodeType = getRelationNodeType(relationAnnotation);

			if (RelationNodeType.TARGET.equals(nodeType)) {
				joinColumnName = RELATION_TARGET_COLUMN;
				inverseJoinColumnName = RELATION_SOURCE_COLUMN;
			}

			joinTableAnn.addMemberValue(MV_JOIN_COLUMNS, createJoinColumn(field, joinColumnName));
			joinTableAnn.addMemberValue(MV_INVERSE_JOIN_COLUMNS, createJoinColumn(field, inverseJoinColumnName));
		}

		return joinTableAnn;
	}

	protected ArrayMemberValue createJoinColumn(final CtField field, final String columnName) {
		final Annotation joinColumnAnn = createAnnotation(field.getFieldInfo2().getConstPool(), JoinColumn.class);

		final StringMemberValue column = new StringMemberValue(field.getFieldInfo2().getConstPool());
		column.setValue(MV_ID);
		joinColumnAnn.addMemberValue(MV_REFERENCED_COLUMN_NAME, column);

		final StringMemberValue name = new StringMemberValue(field.getFieldInfo2().getConstPool());
		name.setValue(columnName);
		joinColumnAnn.addMemberValue(MV_NAME, name);

		final AnnotationMemberValue val = new AnnotationMemberValue(field.getFieldInfo2().getConstPool());
		val.setValue(joinColumnAnn);

		return createAnnotationArrayValue(field.getFieldInfo2().getConstPool(), val);
	}

	protected RelationNodeType getRelationNodeType(final Annotation relationAnnotation) {
		final EnumMemberValue nodeType = (EnumMemberValue) relationAnnotation.getMemberValue(MV_NODE_TYPE);
		return RelationNodeType.valueOf(nodeType.getValue());
	}

	protected Annotation addJpaCascadeAnnotation(final CtField field,
			Class<? extends java.lang.annotation.Annotation> annotationType, boolean addRemoveCascadeType) {

		final Annotation annotation = createAnnotation(field.getFieldInfo2().getConstPool(), annotationType);
		// add fetch type
		final EnumMemberValue fetchType = new EnumMemberValue(field.getFieldInfo2().getConstPool());
		fetchType.setType(FetchType.class.getName());
		fetchType.setValue(FetchType.LAZY.name());
		annotation.addMemberValue("fetch", fetchType);

		final List<EnumMemberValue> vals = new ArrayList<>();

		final List<CascadeType> allowedCascadeTypes = new ArrayList<>();
		allowedCascadeTypes.addAll(Arrays.asList(CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH));

		if (addRemoveCascadeType) {
			allowedCascadeTypes.add(CascadeType.REMOVE);
			annotation.addMemberValue("orphanRemoval", new BooleanMemberValue(true, field.getFieldInfo2().getConstPool()));
		}

		for (CascadeType type : allowedCascadeTypes) {
			final EnumMemberValue val = new EnumMemberValue(field.getFieldInfo2().getConstPool());
			val.setType(CascadeType.class.getName());
			val.setValue(type.toString());

			vals.add(val);
		}

		annotation.addMemberValue(MV_CASCADE, createAnnotationArrayValue(field.getFieldInfo2().getConstPool(),
				vals.toArray(new EnumMemberValue[allowedCascadeTypes.size()])));

		return annotation;
	}

	protected Annotation addHibernateCascadeAnnotation(final CtField field, boolean addRemoveCascadeType) {
		Annotation annotation = createAnnotation(field.getFieldInfo2().getConstPool(), Cascade.class);
		final List<EnumMemberValue> vals = new ArrayList<>();

		final List<org.hibernate.annotations.CascadeType> allowedHibernateCascadeTypes = new ArrayList<>();
		allowedHibernateCascadeTypes.addAll(Arrays.asList(
				org.hibernate.annotations.CascadeType.DETACH, org.hibernate.annotations.CascadeType.SAVE_UPDATE,
				org.hibernate.annotations.CascadeType.LOCK, org.hibernate.annotations.CascadeType.REPLICATE,
				org.hibernate.annotations.CascadeType.MERGE, org.hibernate.annotations.CascadeType.PERSIST,
				org.hibernate.annotations.CascadeType.REFRESH));

		if (addRemoveCascadeType) {
			allowedHibernateCascadeTypes.add(org.hibernate.annotations.CascadeType.REMOVE);
		}

		for (org.hibernate.annotations.CascadeType type : allowedHibernateCascadeTypes) {
			final EnumMemberValue val = new EnumMemberValue(field.getFieldInfo2().getConstPool());
			val.setType(org.hibernate.annotations.CascadeType.class.getName());
			val.setValue(type.toString());

			vals.add(val);
		}

		annotation.addMemberValue(MV_VALUE, createAnnotationArrayValue(field.getFieldInfo2().getConstPool(),
				vals.toArray(new EnumMemberValue[allowedHibernateCascadeTypes.size()])));

		return annotation;
	}

	private String mapColumnTypeToHibernate(final DatabaseColumnType columnType) {
		switch (columnType) {
		case CHAR:
			return "char";
		case VARCHAR:
			return "characters";
		case LONGVARCHAR:
			return "text";
		case CLOB:
			return "clob";
		case BLOB:
			return "blob";
		case TINYINT:
			return "byte";
		case SMALLINT:
			return "short";
		case INTEGER:
			return "int";
		case BIGINT:
			return "long";
		case DOUBLE:
			return "double";
		case FLOAT:
			return "float";
		case NUMERIC:
			return "big_decimal";
		case BIT:
			return "boolean";
		case DATE:
			return "date";
		case TIME:
			return "calendar_time";
		case TIMESTAMP:
			return "calendar";
		case VARBINARY:
			return "binary";
		default:
			return null;
		}
	}
}
