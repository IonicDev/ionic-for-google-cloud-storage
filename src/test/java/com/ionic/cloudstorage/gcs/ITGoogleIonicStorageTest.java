/*
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.gcs;

import static org.junit.Assert.*;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobSourceOption;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.WriteChannel;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.DeviceProfile;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class ITGoogleIonicStorageTest {

    static Logger log = LogManager.getLogger();

    private static Storage googleStorage = null;
    private static GoogleIonicStorage ionicStorage = null;
    private static Agent agent = null;
    private static String testBucket = null;
    private static String testString = null;

    @BeforeClass
    public static void setup() {
        if (TestUtils.googleCredentialsAvailable()) {
            googleStorage = StorageOptions.getDefaultInstance().getService();
            try {
                ionicStorage = new GoogleIonicStorage(TestUtils.getPersistor(), googleStorage);
                agent = TestUtils.getAgent();
            } catch (IonicException e) {
                // Catch any IonicExceptions thrown during setup and null related objects so
                // that dependent tests are each skipped during the preconditions check.
                log.warn(e.getLocalizedMessage());
                ionicStorage = null;
                agent = null;
            }
        }
        testBucket = TestUtils.getTestBucket();
        testString = TestUtils.getTestPayload();
    }

    @Before
    public void preconditions() {
        assertNotNull("Precondition failure, no GoogleStorage client", googleStorage);
        assertNotNull("Precondition failure, no GoogleIonicStorage client", ionicStorage);
        assertNotNull("Precondition failure, no Ionic agent", agent);
        assertNotNull("Precondition failure, no Bucket specified", testBucket);
    }

    @Test
    public void createAndReadAllBytes() {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "createAndReadAllBytes";
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key)).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, testString.getBytes());
        log.info("Reading Blob " + key + " from bucket " + testBucket + " with Google Ionic Storage");
        byte[] blobBytes = ionicStorage.readAllBytes(BlobId.of(testBucket, key));
        assertTrue("Decrypted Blob content does not match original String",
            Arrays.equals(blobBytes, testString.getBytes()));
    }

    @Test
    public void createStreamAndReadAllBytes() {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "createAndReadAllBytes";
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key)).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, IOUtils.toInputStream(testString));
        log.info("Reading Blob " + key + " from bucket " + testBucket + " with Google Ionic Storage");
        byte[] blobBytes = ionicStorage.readAllBytes(BlobId.of(testBucket, key));
        assertTrue("Decrypted Blob content does not match original String",
            Arrays.equals(blobBytes, testString.getBytes()));
    }

    @Test
    public void readerAndWriter() throws IOException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "readerAndWriter";
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key)).setContentType("application/octet-stream").build();
        log.info("Opening WriteChannel for " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        WriteChannel writeChannel = ionicStorage.writer(blobInfo);
        log.info("Writing bytes to " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        writeChannel.write(ByteBuffer.wrap(testString.getBytes()));
        log.info("Closing WriteChannel");
        writeChannel.close();

        log.info("Opening ReadChannel for " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ReadChannel readChannel = ionicStorage.reader(BlobId.of(testBucket, key));
        ByteBuffer readBuffer = ByteBuffer.allocate(testString.getBytes().length);
        log.info("Reading bytes from " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        readChannel.read(readBuffer);
        log.info("Closing ReadChannel");
        readChannel.close();

        assertTrue("Read bytes do not match original String",
            Arrays.equals(readBuffer.array(), testString.getBytes()));
    }

    @Test
    public void metadataCaptureOn() throws IonicException {
        String testMetaKey = "TestMetadataKey";
        String testMetaValue = "TestMetadataValue";
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testMetadataCaptureOn";
        }

        ionicStorage.setEnabledMetadataCapture(true);
        HashMap<String,String> metadata = new HashMap<String,String>();
        metadata.put(testMetaKey, testMetaValue);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key))
            .setMetadata(metadata).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, testString.getBytes());

        log.info("Getting Blob object " + key + " from bucket " + testBucket + " with Google Storage");
        Blob blob = googleStorage.get(BlobId.of(testBucket, key));
        String ionicKeyId = blob.getMetadata().get("ionic-key-id");
        log.info("Getting Ionic Key " + ionicKeyId + " with Ionic Agent");
        GetKeysResponse.Key ionicKey = agent.getKey(ionicKeyId).getFirstKey();

        assertTrue( "Captured metadata value is not equal to \"" + testMetaValue + '"',
            ionicKey.getAttributesMap().get(testMetaKey).get(0).equals(testMetaValue));
    }

    @Test
    public void metadataCaptureOff() throws IonicException {
        String testMetaKey = "TestMetadataKey";
        String testMetaValue = "TestMetadataValue";
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "metadataCaptureOff";
        }

        ionicStorage.setEnabledMetadataCapture(false);
        HashMap<String,String> metadata = new HashMap<String,String>();
        metadata.put(testMetaKey, testMetaValue);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key))
            .setMetadata(metadata).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, testString.getBytes());

        log.info("Getting Blob object " + key + " from bucket " + testBucket + " with Google Storage");
        Blob blob = googleStorage.get(BlobId.of(testBucket, key));
        String ionicKeyId = blob.getMetadata().get("ionic-key-id");
        log.info("Getting Ionic Key " + ionicKeyId + " with Ionic Agent");
        GetKeysResponse.Key ionicKey = agent.getKey(ionicKeyId).getFirstKey();

        assertTrue( "Metadata key \"" + testMetaKey + "\" is present in IonicKey Attributes",
            ionicKey.getAttributesMap().get(testMetaKey) == null);
    }

    @Test
    public void createWithAtrributes() throws IonicException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "createWithAtrributes";
        }

        ionicStorage.setEnabledMetadataCapture(true);

        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        CreateKeysRequest.Key ionicRequestKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key)).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, testString.getBytes(), ionicRequestKey);

        log.info("Getting Blob object " + key + " from bucket " + testBucket + " with Google Storage");
        Blob blob = googleStorage.get(BlobId.of(testBucket, key));
        String ionicKeyId = blob.getMetadata().get("ionic-key-id");
        log.info("Getting Ionic Key " + ionicKeyId + " with Ionic Agent");
        GetKeysResponse.Key ionicKey = agent.getKey(ionicKeyId).getFirstKey();

        assertTrue("Response Key Attributes do not match specified Attributes",
            ionicKey.getAttributesMap().equals(attributes));

        assertTrue("Response Key Mutable Attributes do not match specified Mutable Attributes",
            ionicKey.getMutableAttributesMap().equals(mutableAttributes));

    }

    @Test
    public void createAndReadAllBytesWithAttributes() throws IonicException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "createAndReadAllBytesWithAttributes";
        }

        ionicStorage.setEnabledMetadataCapture(true);

        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        CreateKeysRequest.Key ionicRequestKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key)).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, testString.getBytes(), ionicRequestKey);
        log.info("Reading Blob " + key + " from bucket " + testBucket + " with Google Ionic Storage");
        GoogleIonicStorage.IonicKeyBytesPair pair = ionicStorage.readAllBytesAndKey(BlobId.of(testBucket, key));
        byte[] blobBytes = pair.getByteArray();
        GetKeysResponse.Key ionicKey = pair.getKey();

        assertTrue("Decrypted Blob content does not match original String",
            Arrays.equals(blobBytes, testString.getBytes()));

        assertTrue("Response Key Attributes do not match specified Attributes",
            ionicKey.getAttributesMap().equals(attributes));

        assertTrue("Response Key Mutable Attributes do not match specified Mutable Attributes",
            ionicKey.getMutableAttributesMap().equals(mutableAttributes));
    }

    @Test
    public void unencryptedDownload() {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "unencryptedDownload";
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(testBucket, key)).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Storage");
        googleStorage.create(blobInfo, testString.getBytes());
        log.info("Reading Blob " + key + " from bucket " + testBucket + " with Google Ionic Storage");
        byte[] blobBytes = ionicStorage.readAllBytes(BlobId.of(testBucket, key));
        assertTrue("Unencrypted download did not match source",
        Arrays.equals(blobBytes, testString.getBytes()));

    }

    @Test
    public void getProtectedBlob() {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "getProtectedBlob";
        }

        BlobId blobId = BlobId.of(testBucket, key);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
        log.info("Creating Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.create(blobInfo, testString.getBytes());
        log.info("Getting Blob " + key + " in bucket " + testBucket + " with Google Ionic Storage");
        ionicStorage.get(blobId);
    }

}
