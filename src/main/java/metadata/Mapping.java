package metadata;

public class Mapping {
    private ClassInfo classInfo;

    public Mapping(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public String getTableName() {
        return classInfo.getTableName();
    }

    public FieldInfo getField(String name) {
        for (FieldInfo f : classInfo.getFields()) {
            if (f.getNom().equals(name)) return f;
        }
        return null;
    }
}