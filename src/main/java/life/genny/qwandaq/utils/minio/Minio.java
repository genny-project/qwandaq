package life.genny.qwandaq.utils.minio;


import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.MinioException;
import io.minio.errors.NoResponseException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.constant.MinIOConstant;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class Minio {
    static final Logger log= Logger.getLogger(Minio.class);
    private MinioClient minioClient;
    private String REALM = Optional.ofNullable(System.getenv("REALM")).orElse("internmatch");

    @PostConstruct
    public void beforeConstruct() {
        try {
            minioClient =
                    new MinioClient(
                            MinIOConstant.getServerUrl(),
                            MinIOConstant.getAccessKey(),
                            MinIOConstant.getPrivateKey());
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage());
        }
    }

    public String saveOnStore(FileUpload file) {
        boolean isUploaded = uploadFile(REALM.concat("/") + "public", file.uploadedFileName(), file.fileName());
        if (isUploaded) {
            return file.fileName();
        } else {
            return null;
        }
    }

    public UUID saveOnStore(FileUpload file, UUID userUUID) {
        UUID randomUUID = UUID.randomUUID();
        boolean isUploaded = uploadFile(userUUID.toString(), file.uploadedFileName(), randomUUID.toString());
        if (isUploaded) {
            return randomUUID;
        } else {
            return null;
        }
    }

    public byte[] fetchFromStoreUserDirectory(UUID fileUUID, UUID userUUID) {
        try {
            InputStream object = minioClient.getObject(MinIOConstant.getBucketName(),
                    userUUID.toString() + "/media/" + fileUUID.toString());
            byte[] byteArray = IOUtils.toByteArray(object);
            return byteArray;
        } catch (InvalidKeyException | InvalidBucketNameException
                 | NoSuchAlgorithmException | InsufficientDataException
                 | NoResponseException | ErrorResponseException
                 | InternalException | InvalidArgumentException | IOException
                 | XmlPullParserException e) {
            log.error("Exception: " + e.getMessage());
            return new byte[]{};
        }
    }

    public ObjectStat fetchStatFromStorePublicDirectory(UUID fileUUID) {
        try {
            ObjectStat object = minioClient.statObject(MinIOConstant.getBucketName(),
                    REALM + "/" +
                            "public" + "/" +
                            "media" + "/" +
                            fileUUID.toString());
            return object;
        } catch (InvalidKeyException | InvalidBucketNameException
                 | NoSuchAlgorithmException | InsufficientDataException
                 | NoResponseException | ErrorResponseException
                 | InternalException | IOException
                 | XmlPullParserException e) {
            log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public String fetchInfoFromStorePublicDirectory(UUID fileUUID) {
        try {
            InputStream object = minioClient.getObject(MinIOConstant.getBucketName(),
                    REALM + "/" +
                            "public" + "/" +
                            "media" + "/" +
                            fileUUID.toString().concat("-info"));
            byte[] byteArray = IOUtils.toByteArray(object);
            return new String(byteArray);
        } catch (InvalidKeyException | InvalidBucketNameException
                 | NoSuchAlgorithmException | InsufficientDataException
                 | NoResponseException | ErrorResponseException
                 | InternalException | InvalidArgumentException | IOException
                 | XmlPullParserException e) {
            log.error("Exception: " + e.getMessage());
            return "";
        }
    }


    public byte[] streamFromStorePublicDirectory(UUID fileUUID, Long start, Long end) {
        try {
            InputStream object = minioClient.getObject(MinIOConstant.getBucketName(),
                    REALM + "/" +
                            "public" + "/" +
                            "media" + "/" +
                            fileUUID.toString(), start, end);
            byte[] byteArray = IOUtils.toByteArray(object);
            return byteArray;
        } catch (InvalidKeyException | InvalidBucketNameException
                 | NoSuchAlgorithmException | InsufficientDataException
                 | NoResponseException | ErrorResponseException
                 | InternalException | InvalidArgumentException | IOException
                 | XmlPullParserException e) {
            log.error("Exception: " + e.getMessage());
            return new byte[]{};
        }
    }

    public byte[] fetchFromStorePublicDirectory(UUID fileUUID) {
        try {
            InputStream object = minioClient.getObject(MinIOConstant.getBucketName(),
                    REALM + "/" +
                            "public" + "/" +
                            "media" + "/" +
                            fileUUID.toString());
            byte[] byteArray = IOUtils.toByteArray(object);
            return byteArray;
        } catch (InvalidKeyException | InvalidBucketNameException
                 | NoSuchAlgorithmException | InsufficientDataException
                 | NoResponseException | ErrorResponseException
                 | InternalException | InvalidArgumentException | IOException
                 | XmlPullParserException e) {
            log.error("Exception: " + e.getMessage());
            return new byte[]{};
        }
    }

    public byte[] fetchFromStorePublicDirectory(String fileName) {
        try {
            InputStream object = minioClient.getObject(MinIOConstant.getBucketName(),
                    REALM + "/" +
                            "public" + "/" +
                            "media" + "/" +
                            fileName);
            byte[] byteArray = IOUtils.toByteArray(object);
            return byteArray;
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage());
            return new byte[]{};
        }
    }

    public void deleteFromStorePublicDirectory(UUID fileUUID) {
        try {
            minioClient.removeObject(MinIOConstant.getBucketName(),
                    REALM + "/" +
                            "public" + "/" +
                            "media" + "/" +
                            fileUUID.toString());
        } catch (InvalidKeyException | InvalidBucketNameException
                 | NoSuchAlgorithmException | InsufficientDataException
                 | NoResponseException | ErrorResponseException
                 | InternalException | InvalidArgumentException | IOException
                 | XmlPullParserException e) {
            log.error("Exception: " + e.getMessage());
        }
    }

    public boolean uploadFile(String sub, String inpt, String uuid) {
        boolean isSuccess = false;

        String path = sub + "/" + "media" + "/" + uuid;
        try {
            boolean isExist = minioClient.bucketExists(MinIOConstant.getBucketName());
            if (isExist) {
                log.info("Bucket " + MinIOConstant.getBucketName() + "already exists.");
            } else {
                log.info("Start creat Bucket:" + MinIOConstant.getBucketName());
                minioClient.makeBucket(MinIOConstant.getBucketName());
                log.info("Finish create Bucket:" + MinIOConstant.getBucketName());
            }

            minioClient.putObject(MinIOConstant.getBucketName(), path, inpt);
            isSuccess = true;
            log.info("Success, File" + inpt + " uploaded to bucket with path:" + path);
        } catch (MinioException | InvalidKeyException
                 | NoSuchAlgorithmException | IOException
                 | XmlPullParserException e) {
            log.error("Error occurred when upload file to bucket: " + e.getMessage());
        }
        return isSuccess;
    }
}