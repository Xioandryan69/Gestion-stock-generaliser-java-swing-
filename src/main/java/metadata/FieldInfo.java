package metadata;

public class FieldInfo {

    private String nom;
    private Class<?> type;          //Adrresse.class
    private boolean enumType;       //enum
    private boolean primitiveType;  //int ,double ,float ,String
    private boolean objectType;     //true si classe crée
    private boolean listType;       //type Array ou nom 
    private Class<?> genericType;   //classe crée Addresse.java
    private TypeField typeField; 
    private boolean ignored;
    private boolean idField;

    // getters / setters
    public boolean isIgnored() { return ignored; }
    public void setIgnored(boolean ignored) { this.ignored = ignored; }
    public boolean isIdField() { return idField; }
    public void setIdField(boolean idField) { this.idField = idField; }
    
    public TypeField getTypeField() { return typeField; } 
    public void setTypeField(TypeField typeField) { this.typeField = typeField; }

    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public Class<?> getType() {
        return type;
    }
    public void setType(Class<?> type) {
        this.type = type;
    }
    public boolean isEnumType() {
        return enumType;
    }
    public void setEnumType(boolean enumType) {
        this.enumType = enumType;
    }
    public boolean isPrimitiveType() {
        return primitiveType;
    }
    public void setPrimitiveType(boolean primitiveType) {
        this.primitiveType = primitiveType;
    }
    public boolean isObjectType() {
        return objectType;
    }
    public void setObjectType(boolean objectType) {
        this.objectType = objectType;
    }
    public boolean isListType() {
        return listType;
    }
    public void setListType(boolean listType) {
        this.listType = listType;
    }
    public Class<?> getGenericType() {
        return genericType;
    }
    public void setGenericType(Class<?> genericType) {
        this.genericType = genericType;
    }

}

