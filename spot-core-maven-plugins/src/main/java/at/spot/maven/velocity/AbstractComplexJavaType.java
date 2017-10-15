package at.spot.maven.velocity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractComplexJavaType extends AbstractJavaObject {
	private static final long serialVersionUID = 1L;

	protected final List<String> imports = new ArrayList<>();
	protected final List<JavaInterface> interfaces = new ArrayList<>();
	protected final List<JavaMethod> methods = new ArrayList<>();

	protected String packagePath;
	protected JavaInterface superClass;

	public String getPackagePath() {
		return packagePath;
	}

	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}

	public JavaInterface getSuperClass() {
		return superClass;
	}

	public void setSuperClass(JavaInterface superClass) {
		this.superClass = superClass;
		this.imports.add(superClass.getFullyQualifiedName());
	}

	public void setSuperClass(Class<?> superClass) {
		JavaClass javaSuperClass = new JavaClass();
		javaSuperClass.setPackagePath(superClass.getPackage().getName());
		javaSuperClass.setName(superClass.getSimpleName());

		this.imports.add(superClass.getName());
	}

	public List<JavaInterface> getInterfaces() {
		return Collections.unmodifiableList(interfaces);
	}

	public List<JavaMethod> getMethods() {
		return Collections.unmodifiableList(methods);
	}

	public void addInterface(JavaInterface iface) {
		this.interfaces.add(iface);
		this.imports.add(iface.getFullyQualifiedName());
	}

	public void addMethod(JavaMethod method) {
		this.methods.add(method);

		for (Map.Entry<String, Class<?>> entry : method.getArguments().entrySet()) {
			this.imports.add(entry.getValue().getName());
		}

		if (method.isComplexType()) {
			this.imports.add(method.getComplexType().getName());
		}
	}

	public void addAnnotation(JavaAnnotation annotation) {
		this.annotations.add(annotation);
		this.imports.add(annotation.getClass().getName());
	}

	public String getFullyQualifiedName() {
		return this.packagePath + "." + this.getName();
	}
}
