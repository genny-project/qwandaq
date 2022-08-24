package life.genny.qwandaq.constant;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MinIOConstant {

    public static String getServerUrl(){
        return System.getenv("MINIO_SERVER_URL") != null
                ? System.getenv("MINIO_SERVER_URL")
                : "MINIO_SERVER_URL";
    }

    public static String getAccessKey(){
        return System.getenv("MINIO_ACCESS_KEY") != null
                ? System.getenv("MINIO_ACCESS_KEY")
                : "MINIO_ACCESS_KEY";
    }

    public static String getPrivateKey(){
        return System.getenv("MINIO_SECRET_KEY") != null
                ? System.getenv("MINIO_SECRET_KEY")
                : "MINIO_SECRET_KEY";
    }

    public static String getBucketName(){
        return System.getenv("BUCKET_NAME") != null
                ? System.getenv("BUCKET_NAME")
                : "BUCKET_NAME";
    }
}
