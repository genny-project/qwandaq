package life.genny.qwandaq.converter;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.constant.QwandaQConstant;
import life.genny.qwandaq.utils.minio.Minio;
import org.jboss.logging.Logger;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class MinIOConverter implements AttributeConverter<String, String> {

    static final Logger log = Logger.getLogger(MinIOConverter.class);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData != null && dbData.startsWith(QwandaQConstant.MINIO_LAZY_PREFIX)) {
            log.info("Fetching from MinIO");

            byte[] data = Arc.container().instance(Minio.class).get().fetchFromStorePublicDirectory(dbData);
            if (data.length > 0) {
                return new String(data);
            } else {
                return "Error Occurred";
            }
        } else {
            return dbData;
        }
    }
}