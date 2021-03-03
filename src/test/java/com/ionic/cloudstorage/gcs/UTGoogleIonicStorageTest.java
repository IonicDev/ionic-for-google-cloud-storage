/*
 * (c) 2017-2021 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.gcs;

import static org.junit.Assert.*;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.Storage.BlobSourceOption;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.DeviceProfile;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Base64;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

public class UTGoogleIonicStorageTest {

    private byte[] keyBytes = new byte[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};
    private CreateKeysResponse.Key createKeyStub = new CreateKeysResponse.Key("", "", keyBytes, "");
    private GetKeysResponse.Key getKeyStub = new GetKeysResponse.Key("", keyBytes, "");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // External method tests

    @Test
    public void constructorAndSetPersistor() throws IonicException {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);

        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();

        ionicStorage.setPersistor(ptPersistor);
    }

    @Test
    public void constructorWithPersistor() throws IonicException {
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        Storage googleStorage = new StorageStub();

        new GoogleIonicStorage(ptPersistor, googleStorage);
    }

    @Test
    public void constructorAndSetAgent() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);

        Agent agent = new Agent();

        ionicStorage.setAgent(agent);
    }

    @Test
    public void constructorWithAgent() throws IonicException {
        Agent agent = new Agent();
        Storage googleStorage = new StorageStub();

        new GoogleIonicStorage(agent, googleStorage);
    }

    @Test
    public void setGetMetaDataCapture() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        assertFalse("MetadataCapture was not false by default.", ionicStorage.isEnabledMetadataCapture());
        ionicStorage.setEnabledMetadataCapture(true);
        assertTrue("MetadataCapture was not true after iemp.setEnabledMetadataCapture(true)",
            ionicStorage.isEnabledMetadataCapture());
    }

    @Test
    public void setGetDefaultMetadata() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        assertTrue("DefaultAttributes were not empty by default.",
            ionicStorage.getDefaultAttributes().isEmpty());

        KeyAttributesMap kam = new KeyAttributesMap();
        ArrayList<String> collection = new ArrayList<String>();
        collection.add("confidential");
        collection.add("secured");
        kam.put("privacy", collection);

        ionicStorage.setDefaultAttributes(kam);
        assertEquals("getDefaultAttributes() did not equal map set with setDefaultAttributes()",
            ionicStorage.getDefaultAttributes(), kam);
    }

    @Test
    public void setGetAgentMetadata() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        assertTrue("IonicMetadataMap was not empty by default.",
            ionicStorage.getIonicMetadataMap().isEmpty());

        MetadataMap metaMap = new MetadataMap();
        metaMap.set("ionic-application-name", "Unit_Test");
        metaMap.set("ionic-application-version", "0.0.0");

        ionicStorage.setIonicMetadataMap(metaMap);
        assertEquals("getIonicMetadataMap() did not equal map set with setIonicMetadataMap()",
            ionicStorage.getIonicMetadataMap(), metaMap);
    }

    // Internal method tests

    @Test
    public void writeOptionsWithEncrytion() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);

        BlobWriteOption[] optionsArray = ionicStorage.writeOptionsWithEncrytion(createKeyStub);
        assertTrue("Returned Options Array length is not 1", optionsArray.length == 1);
        Base64.Encoder encoder = Base64.getEncoder();
        assertTrue("Option does not match expected value",
            optionsArray[0].equals(BlobWriteOption.encryptionKey(encoder.encodeToString(createKeyStub.getKey()))));
    }

    @Test
    public void writeOptionsWithEncrytionAndOptions() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        BlobWriteOption[] initialOptionsArray = new BlobWriteOption[] {BlobWriteOption.doesNotExist()};

        BlobWriteOption[] optionsArray = ionicStorage.writeOptionsWithEncrytion(createKeyStub, initialOptionsArray);
        assertTrue("Returned Options Array length is not 2", optionsArray.length == 2);
        Base64.Encoder encoder = Base64.getEncoder();
        assertTrue("Option does not match expected value",
            optionsArray[0].equals(BlobWriteOption.encryptionKey(encoder.encodeToString(createKeyStub.getKey()))));
    }

    @Test
    public void targetOptionsWithEncrytion() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);

        BlobTargetOption[] optionsArray = ionicStorage.targetOptionsWithEncrytion(createKeyStub);
        assertTrue("Returned Options Array length is not 1", optionsArray.length == 1);
        Base64.Encoder encoder = Base64.getEncoder();
        assertTrue("Option does not match expected value",
            optionsArray[0].equals(BlobTargetOption.encryptionKey(encoder.encodeToString(createKeyStub.getKey()))));
    }

    @Test
    public void targetOptionsWithEncrytionAndOptions() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        BlobTargetOption[] initialOptionsArray = new BlobTargetOption[] {BlobTargetOption.doesNotExist()};

        BlobTargetOption[] optionsArray = ionicStorage.targetOptionsWithEncrytion(createKeyStub, initialOptionsArray);
        assertTrue("Returned Options Array length is not 2", optionsArray.length == 2);
        Base64.Encoder encoder = Base64.getEncoder();
        assertTrue("Option does not match expected value",
            optionsArray[0].equals(BlobTargetOption.encryptionKey(encoder.encodeToString(createKeyStub.getKey()))));
    }

    @Test
    public void sourceOptionsWithDecryption() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);

        BlobSourceOption[] optionsArray = ionicStorage.sourceOptionsWithDecryption(getKeyStub);
        assertTrue("Returned Options Array length is not 1", optionsArray.length == 1);
        Base64.Encoder encoder = Base64.getEncoder();
        assertTrue("Option does not match expected value",
            optionsArray[0].equals(BlobSourceOption.decryptionKey(encoder.encodeToString(getKeyStub.getKey()))));
    }

    @Test
    public void sourceOptionsWithDecryptionAndOptions() {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        BlobSourceOption[] initialOptionsArray = new BlobSourceOption[] {BlobSourceOption.generationMatch()};

        BlobSourceOption[] optionsArray = ionicStorage.sourceOptionsWithDecryption(getKeyStub, initialOptionsArray);
        assertTrue("Returned Options Array length is not 2", optionsArray.length == 2);
        Base64.Encoder encoder = Base64.getEncoder();
        assertTrue("Option does not match expected value",
            optionsArray[0].equals(BlobSourceOption.decryptionKey(encoder.encodeToString(getKeyStub.getKey()))));
    }

    @Test
    public void testNoOp() throws MalformedURLException {
        Storage googleStorage = new StorageStub();
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(googleStorage);
        URL url = new URL("http://ionic.com");
        thrown.expect(StorageException.class);
        thrown.expectMessage("Unsupported Method");
        ionicStorage.writer(url);
    }

}
