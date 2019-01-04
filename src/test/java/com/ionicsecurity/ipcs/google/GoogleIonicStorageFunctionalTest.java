/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the Terms & Conditions (https://dev.ionic.com/use.html)
 * and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.google;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.StorageOptions;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;

import static com.ionicsecurity.ipcs.google.GoogleIonicStorage.IONICMETACONSTANT;

public class GoogleIonicStorageFunctionalTest {

    private static String bucketName = System.getenv("IPCS_GCS_TEST_BUCKET");
    private static String blobName = "test";
    private static GoogleIonicStorage ionicStorage;
    private static String blobContent;
    private static byte[] oriBlobBytes;
    private static BlobInfo blobInfo;
    private static DeviceProfilePersistorPlainText profilePersistor;

    @BeforeClass
    public static void init() throws UnsupportedEncodingException, IonicException {
        blobContent = "This is the functional test blob";
        blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).build();
        oriBlobBytes = blobContent.getBytes("UTF-8");

        profilePersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        profilePersistor.setFilePath(sProfilePath);

        ionicStorage = new GoogleIonicStorage(profilePersistor, StorageOptions.getDefaultInstance().getService());
    }

    @Test
    public void createAndReadRoundtrip() throws UnsupportedEncodingException {
        byte[] preUploadedBytes;
        preUploadedBytes = blobContent.getBytes("UTF-8");
        ionicStorage.create(blobInfo, preUploadedBytes, (BlobTargetOption) null);
        byte[] blobBytes = ionicStorage.readAllBytes(bucketName, blobName);
        String message = "Create and Read Roundtrip Failed";
        assertArrayEquals(message, oriBlobBytes, blobBytes);
    }

    @Test
    public void createAndReaderRoundtrip() throws IOException {
        ionicStorage.create(blobInfo, blobContent.getBytes());
        ByteBuffer dlContent = ByteBuffer.allocate(oriBlobBytes.length);
        ReadChannel readChan = ionicStorage.reader(bucketName, blobName);
        readChan.read(dlContent);
        dlContent.position(0);

        ByteBuffer orig = ByteBuffer.wrap(oriBlobBytes);
        assertEquals(orig.compareTo(dlContent), 0);
    }

    @Test
    public void writerAndRead() throws IOException {
        WriteChannel writer = ionicStorage.writer(blobInfo);
        try {
            writer.write(ByteBuffer.wrap(oriBlobBytes, 0, oriBlobBytes.length));
        } finally {
            writer.close();
        }

        byte[] blobBytes = ionicStorage.readAllBytes(bucketName, blobName);
        String message = "Writer and Read roundtrip Failed";
        assertArrayEquals(message, oriBlobBytes, blobBytes);
    }

    @Test
    public void testUpdateAgainstMetaCurruption() {
        String badIonicKey = "gibberish";
        HashMap<String, String> blobInfoMetadata = new HashMap<String, String>();
        blobInfoMetadata.put(IONICMETACONSTANT, badIonicKey);

        BlobInfo newBlobInfo = BlobInfo.newBuilder(blobInfo.getBlobId()).setMetadata(blobInfoMetadata).build();
        Blob blob = ionicStorage.update(newBlobInfo);
        String ionicKey = blob.getMetadata().get(IONICMETACONSTANT);
        assertFalse(ionicKey == badIonicKey);
    }
}