package life.genny.qwandaq.message;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class QDataMessage extends QMessage implements QDataMessageIntf {

  @Override
  public String getData_type() {
    return data_type;
  }

  public void setData_type(final String data_type) {
    this.data_type = data_type;
  }

  /**
   *
   */
  private static final String MESSAGE_TYPE = "DATA_MSG";
  private String data_type;
  private boolean delete;
  private boolean replace;
  private boolean shouldDeleteLinkedBaseEntities;
  private String aliasCode;

  public QDataMessage() {
    super();
  }

  public QDataMessage(final String data_type) {
    super(MESSAGE_TYPE);
    this.data_type = data_type;

  }

  @Override
  public Boolean getDelete() {
    return delete;
  }

  public void setDelete(final Boolean delete) {
    this.delete = delete;
  }

  @Override
  public String toString() {
    return "QDataMessage [data_type=" + data_type + ", delete=" + delete + "]";
  }

  /**
   * @return the aliasCode
   */
  public String getAliasCode() {
    return aliasCode;
  }

  /**
   * @param aliasCode the aliasCode to set
   */
  public void setAliasCode(final String aliasCode) {
    this.aliasCode = aliasCode;
  }

  /**
   * @return the replace
   */
  public boolean getReplace() {
    return replace;
  }

  /**
   * @param replace the replace to set
   */
  public void setReplace(boolean replace) {
    this.replace = replace;
  }

  /**
   * @return the shouldDeleteLinkedBaseEntities
   */
  public Object getShouldDeleteLinkedBaseEntities() {
    return shouldDeleteLinkedBaseEntities;
  }

  public boolean isShouldDeleteLinkedBaseEntities() {
    return (boolean) getShouldDeleteLinkedBaseEntities();
  }

  /**
   * @param shouldDeleteLinkedBaseEntities the shouldDeleteLinkedBaseEntities to
   *                                       set
   */
  public void setShouldDeleteLinkedBaseEntities(Object shouldDeleteLinkedBaseEntities) {
    this.shouldDeleteLinkedBaseEntities = (Boolean) shouldDeleteLinkedBaseEntities;
  }

}
