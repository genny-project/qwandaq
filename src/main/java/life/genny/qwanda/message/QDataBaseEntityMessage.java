package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RegisterForReflection
public class QDataBaseEntityMessage extends QDataMessage {

    private static final long serialVersionUID = 1L;
    private BaseEntity[] items = new BaseEntity[0];
    private static final String DATATYPE_ATTRIBUTE = Attribute.class.getSimpleName();
    private static final String DATATYPE_BASEENTITY = BaseEntity.class.getSimpleName();


    public QDataBaseEntityMessage() {
        super(DATATYPE_ATTRIBUTE);
    }

    public QDataBaseEntityMessage(BaseEntity[] items) {
        super(DATATYPE_ATTRIBUTE);
        setItems(items);
    }

    public QDataBaseEntityMessage(final BaseEntity item) {
        this(item, null);
    }

    public QDataBaseEntityMessage(final BaseEntity item, final String alias) {
        super(DATATYPE_BASEENTITY);
        items = new BaseEntity[1];
        items[0] = item;
        setItems(items);
        setAliasCode(alias);
    }

    public QDataBaseEntityMessage(final BaseEntity[] items, final String parentCode,
                                  final String linkCode) {
        super(DATATYPE_BASEENTITY);
        this.items = items;
        this.parentCode = parentCode;
        this.linkCode = linkCode;
    }


    public BaseEntity[] getItems() {
        return items;
    }

    public void setItems(BaseEntity[] items) {
        this.items = items;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    @Expose
    private String parentCode;


    public String getLinkCode() {
        return linkCode;
    }

    public void setLinkCode(String linkCode) {
        this.linkCode = linkCode;
    }

    @Expose
    private String linkCode;


    public void add(BaseEntity item) {

        List<BaseEntity> bes = this.getItems() != null ? new CopyOnWriteArrayList<>(Arrays.asList(this.getItems())) : new CopyOnWriteArrayList<>();
        bes.add(item);
        this.setItems(bes.toArray(new BaseEntity[0]));
    }

}
