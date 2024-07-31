package project.homelearn.config.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static project.homelearn.config.storage.StorageConstants.NCP_STORAGE_URL;
import static project.homelearn.config.storage.StorageConstants.REGION_NAME;

@Configuration
public class StorageConfig {

    @Value("${access.key}")
    private String accessKey;

    @Value("${secret.key}")
    private String secretKey;

    @Bean
    public AmazonS3Client amazonS3Client() {

        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return (AmazonS3Client) AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(NCP_STORAGE_URL, REGION_NAME))
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .build();
    }
}