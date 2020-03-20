/*
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.gcs;

import static org.junit.Assert.*;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

import com.ionic.sdk.error.IonicException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

public class ITGoogleIonicStorageDeniedTest {

    static Logger log = LogManager.getLogger();

    private static GoogleIonicStorage ionicStorage = null;
    private static String testBucket = null;
    private static String testString = null;

    @BeforeClass
    public static void setup() {
        if (TestUtils.googleCredentialsAvailable()) {
            Storage googleStorage = StorageOptions.getDefaultInstance().getService();
            try {
                ionicStorage = new GoogleIonicStorage(TestUtils.getPersistor(), googleStorage);
            } catch (IonicException e) {
                // Catch any IonicExceptions thrown during setup and null related objects so
                // that dependent tests are each skipped during the preconditions check.
                log.warn(e.getLocalizedMessage());
                ionicStorage = null;
            }
        }
        testBucket = TestUtils.getTestBucket();
        testString = TestUtils.getTestPayload();
    }

    @Before
    public void preconditions() {
        assertNotNull("Precondition failure, no GoogleIonicStorage client", ionicStorage);
        assertNotNull("Precondition failure, no Bucket specified", testBucket);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

        thrown.expect(StorageException.class);
        thrown.expectMessage("40024 - Key fetch or creation was denied by the server");

        byte[] blobBytes = ionicStorage.readAllBytes(BlobId.of(testBucket, key));
    }

}
