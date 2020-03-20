/*
 * (c) 2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.samples;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.ionic.cloudstorage.gcs.GoogleIonicStorage;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPassword;
import com.ionic.sdk.error.IonicException;

/**
 * A hello world example using the GoogleIonicStorage client.
 */
public class HelloWorld {

    private static final String HOME = System.getProperty("user.home");

    public static void main(String... args) {
        // read persistor password from environment variable
        String persistorPassword = System.getenv("IONIC_PERSISTOR_PASSWORD");
        if (persistorPassword == null) {
            System.out.println("[!] Please provide the persistor password as env variable:"
                    + " IONIC_PERSISTOR_PASSWORD");
            System.exit(1);
        }

        // initialize agent
        Agent agent = new Agent();
        try {
            String persistorPath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pw";
            DeviceProfilePersistorPassword persistor =
                    new DeviceProfilePersistorPassword(persistorPath);
            persistor.setPassword(persistorPassword);
            agent.initialize(persistor);
        } catch (IonicException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // initialize a new GoogleIonicStorage with an Ionic Agent and the default Google Storage
        // getDefaultInstance() uses the .json file specified by GOOGLE_APPLICATION_CREDENTIALS
        // in the environment
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(agent,
                StorageOptions.getDefaultInstance().getService());

        // create a BlobId using your previously created bucket and new blob key
        BlobId blobId = BlobId.of("my-unique-bucket", "my-blob-key");
        // build a BlobInfo from the BlobId and specify the content type of the blob
        BlobInfo blobInfo =
                BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
        // use the create method on the ionicStorage object to upload the blob with a byte[] payload
        try {
            ionicStorage.create(blobInfo, "Hello, World!".getBytes(UTF_8));
        } catch (StorageException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // download the blob using the readAllBytes method and the blobId
        byte[] downloadBytes = null;
        try {
            downloadBytes = ionicStorage.readAllBytes(blobId);
        } catch (StorageException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // convert the bytes to a string and print it to standard out
        System.out.println(new String(downloadBytes));

        // exit
        System.exit(0);
    }
}
