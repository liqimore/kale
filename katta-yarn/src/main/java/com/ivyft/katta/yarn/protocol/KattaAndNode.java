/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.ivyft.katta.yarn.protocol;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class KattaAndNode extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"KattaAndNode\",\"namespace\":\"com.ivyft.katta.yarn.protocol\",\"fields\":[{\"name\":\"nodeName\",\"type\":\"string\"},{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"NodeType\",\"symbols\":[\"KATTA_MASTER\",\"KATTA_NODE\"]}},{\"name\":\"appAttemptId\",\"type\":\"string\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public java.lang.CharSequence nodeName;
  @Deprecated public com.ivyft.katta.yarn.protocol.NodeType type;
  @Deprecated public java.lang.CharSequence appAttemptId;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public KattaAndNode() {}

  /**
   * All-args constructor.
   */
  public KattaAndNode(java.lang.CharSequence nodeName, com.ivyft.katta.yarn.protocol.NodeType type, java.lang.CharSequence appAttemptId) {
    this.nodeName = nodeName;
    this.type = type;
    this.appAttemptId = appAttemptId;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return nodeName;
    case 1: return type;
    case 2: return appAttemptId;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: nodeName = (java.lang.CharSequence)value$; break;
    case 1: type = (com.ivyft.katta.yarn.protocol.NodeType)value$; break;
    case 2: appAttemptId = (java.lang.CharSequence)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'nodeName' field.
   */
  public java.lang.CharSequence getNodeName() {
    return nodeName;
  }

  /**
   * Sets the value of the 'nodeName' field.
   * @param value the value to set.
   */
  public void setNodeName(java.lang.CharSequence value) {
    this.nodeName = value;
  }

  /**
   * Gets the value of the 'type' field.
   */
  public com.ivyft.katta.yarn.protocol.NodeType getType() {
    return type;
  }

  /**
   * Sets the value of the 'type' field.
   * @param value the value to set.
   */
  public void setType(com.ivyft.katta.yarn.protocol.NodeType value) {
    this.type = value;
  }

  /**
   * Gets the value of the 'appAttemptId' field.
   */
  public java.lang.CharSequence getAppAttemptId() {
    return appAttemptId;
  }

  /**
   * Sets the value of the 'appAttemptId' field.
   * @param value the value to set.
   */
  public void setAppAttemptId(java.lang.CharSequence value) {
    this.appAttemptId = value;
  }

  /** Creates a new KattaAndNode RecordBuilder */
  public static com.ivyft.katta.yarn.protocol.KattaAndNode.Builder newBuilder() {
    return new com.ivyft.katta.yarn.protocol.KattaAndNode.Builder();
  }
  
  /** Creates a new KattaAndNode RecordBuilder by copying an existing Builder */
  public static com.ivyft.katta.yarn.protocol.KattaAndNode.Builder newBuilder(com.ivyft.katta.yarn.protocol.KattaAndNode.Builder other) {
    return new com.ivyft.katta.yarn.protocol.KattaAndNode.Builder(other);
  }
  
  /** Creates a new KattaAndNode RecordBuilder by copying an existing KattaAndNode instance */
  public static com.ivyft.katta.yarn.protocol.KattaAndNode.Builder newBuilder(com.ivyft.katta.yarn.protocol.KattaAndNode other) {
    return new com.ivyft.katta.yarn.protocol.KattaAndNode.Builder(other);
  }
  
  /**
   * RecordBuilder for KattaAndNode instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<KattaAndNode>
    implements org.apache.avro.data.RecordBuilder<KattaAndNode> {

    private java.lang.CharSequence nodeName;
    private com.ivyft.katta.yarn.protocol.NodeType type;
    private java.lang.CharSequence appAttemptId;

    /** Creates a new Builder */
    private Builder() {
      super(com.ivyft.katta.yarn.protocol.KattaAndNode.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(com.ivyft.katta.yarn.protocol.KattaAndNode.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.nodeName)) {
        this.nodeName = data().deepCopy(fields()[0].schema(), other.nodeName);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.type)) {
        this.type = data().deepCopy(fields()[1].schema(), other.type);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.appAttemptId)) {
        this.appAttemptId = data().deepCopy(fields()[2].schema(), other.appAttemptId);
        fieldSetFlags()[2] = true;
      }
    }
    
    /** Creates a Builder by copying an existing KattaAndNode instance */
    private Builder(com.ivyft.katta.yarn.protocol.KattaAndNode other) {
            super(com.ivyft.katta.yarn.protocol.KattaAndNode.SCHEMA$);
      if (isValidValue(fields()[0], other.nodeName)) {
        this.nodeName = data().deepCopy(fields()[0].schema(), other.nodeName);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.type)) {
        this.type = data().deepCopy(fields()[1].schema(), other.type);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.appAttemptId)) {
        this.appAttemptId = data().deepCopy(fields()[2].schema(), other.appAttemptId);
        fieldSetFlags()[2] = true;
      }
    }

    /** Gets the value of the 'nodeName' field */
    public java.lang.CharSequence getNodeName() {
      return nodeName;
    }
    
    /** Sets the value of the 'nodeName' field */
    public com.ivyft.katta.yarn.protocol.KattaAndNode.Builder setNodeName(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.nodeName = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'nodeName' field has been set */
    public boolean hasNodeName() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'nodeName' field */
    public com.ivyft.katta.yarn.protocol.KattaAndNode.Builder clearNodeName() {
      nodeName = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'type' field */
    public com.ivyft.katta.yarn.protocol.NodeType getType() {
      return type;
    }
    
    /** Sets the value of the 'type' field */
    public com.ivyft.katta.yarn.protocol.KattaAndNode.Builder setType(com.ivyft.katta.yarn.protocol.NodeType value) {
      validate(fields()[1], value);
      this.type = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'type' field has been set */
    public boolean hasType() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'type' field */
    public com.ivyft.katta.yarn.protocol.KattaAndNode.Builder clearType() {
      type = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'appAttemptId' field */
    public java.lang.CharSequence getAppAttemptId() {
      return appAttemptId;
    }
    
    /** Sets the value of the 'appAttemptId' field */
    public com.ivyft.katta.yarn.protocol.KattaAndNode.Builder setAppAttemptId(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.appAttemptId = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'appAttemptId' field has been set */
    public boolean hasAppAttemptId() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'appAttemptId' field */
    public com.ivyft.katta.yarn.protocol.KattaAndNode.Builder clearAppAttemptId() {
      appAttemptId = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public KattaAndNode build() {
      try {
        KattaAndNode record = new KattaAndNode();
        record.nodeName = fieldSetFlags()[0] ? this.nodeName : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.type = fieldSetFlags()[1] ? this.type : (com.ivyft.katta.yarn.protocol.NodeType) defaultValue(fields()[1]);
        record.appAttemptId = fieldSetFlags()[2] ? this.appAttemptId : (java.lang.CharSequence) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}