/*
 * (c) 2017-2018 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.google;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
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
  public static void init()
      throws InvalidPathException, IOException, UnsupportedEncodingException, IonicException {
    blobContent = "This is the functional test blob";
    blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).build();
    oriBlobBytes = blobContent.getBytes("UTF-8");

    profilePersistor = new DeviceProfilePersistorPlainText();
    String sProfilePath = Paths.get(System.getProperty("user.home") + "/.ionicsecurity/profiles.pt")
        .toFile().getCanonicalPath();
    profilePersistor.setFilePath(sProfilePath);

    ionicStorage =
        new GoogleIonicStorage(profilePersistor, StorageOptions.getDefaultInstance().getService());
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
  public void createWithStreamAndReaderRoundtrip() throws IOException {
    ionicStorage.create(blobInfo, new ByteArrayInputStream(blobContent.getBytes()));
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
  public void attributeRoundtripStreamReader() throws IOException {
    KeyAttributesMap attributes = new KeyAttributesMap();
    KeyAttributesMap mutableAttributes = new KeyAttributesMap();
    attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
    mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));

    CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
    ionicStorage.create(blobInfo, new ByteArrayInputStream(blobContent.getBytes()), reqKey);
    ByteBuffer dlContent = ByteBuffer.allocate(oriBlobBytes.length);
    GoogleIonicStorage.IonicKeyReadChannelPair pair =
        ionicStorage.readerAndKey(bucketName, blobName);
    ReadChannel readChan = pair.getReadChannel();
    readChan.read(dlContent);
    dlContent.position(0);

    assertTrue(pair.getKey().getAttributesMap().equals(attributes));
    assertTrue(pair.getKey().getMutableAttributesMap().equals(mutableAttributes));

    ByteBuffer orig = ByteBuffer.wrap(oriBlobBytes);
    assertEquals(orig.compareTo(dlContent), 0);
  }

  @Test
  public void attributeRoundtripCreateReadAll() throws UnsupportedEncodingException {
    KeyAttributesMap attributes = new KeyAttributesMap();
    KeyAttributesMap mutableAttributes = new KeyAttributesMap();
    attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
    mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));

    CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
    byte[] preUploadedBytes = blobContent.getBytes("UTF-8");
    ionicStorage.create(blobInfo, preUploadedBytes, reqKey, (BlobTargetOption) null);
    GoogleIonicStorage.IonicKeyBytesPair pair =
        ionicStorage.readAllBytesAndKey(bucketName, blobName);
    byte[] blobBytes = pair.getByteArray();
    String message = "Create and Read with Attributes Roundtrip Failed";
    assertTrue(pair.getKey().getAttributesMap().equals(attributes));
    assertTrue(pair.getKey().getMutableAttributesMap().equals(mutableAttributes));
    assertArrayEquals(message, oriBlobBytes, blobBytes);
  }

  @Test
  public void attributeRoundtripWriterReader() throws IOException {
    KeyAttributesMap attributes = new KeyAttributesMap();
    KeyAttributesMap mutableAttributes = new KeyAttributesMap();
    attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
    mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));

    CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

    WriteChannel writer = ionicStorage.writer(blobInfo, reqKey);
    try {
      writer.write(ByteBuffer.wrap(oriBlobBytes, 0, oriBlobBytes.length));
    } finally {
      writer.close();
    }

    ByteBuffer dlContent = ByteBuffer.allocate(oriBlobBytes.length);
    GoogleIonicStorage.IonicKeyReadChannelPair pair =
        ionicStorage.readerAndKey(bucketName, blobName);
    ReadChannel readChan = pair.getReadChannel();
    readChan.read(dlContent);
    dlContent.position(0);

    assertTrue(pair.getKey().getAttributesMap().equals(attributes));
    assertTrue(pair.getKey().getMutableAttributesMap().equals(mutableAttributes));

    ByteBuffer orig = ByteBuffer.wrap(oriBlobBytes);
    assertEquals(orig.compareTo(dlContent), 0);
  }

  @Test
  public void testMetaDataPreservation() throws IOException {
    byte[] preUploadedBytes = blobContent.getBytes("UTF-8");
    ionicStorage.create(blobInfo, preUploadedBytes, (BlobTargetOption) null);
    byte[] blobBytes = ionicStorage.readAllBytes(bucketName, blobName);
    String message = "Create and Read Roundtrip Failed";
    assertArrayEquals(message, oriBlobBytes, blobBytes);
    HashMap<String, String> newMetadata = new HashMap<>();
    newMetadata.put("key", "value");
    BlobInfo updatedBlobInfo =
        BlobInfo.newBuilder(blobInfo.getBlobId()).setMetadata(newMetadata).build();
    ionicStorage.update(updatedBlobInfo);
    blobBytes = ionicStorage.readAllBytes(bucketName, blobName);
    assertArrayEquals(message, oriBlobBytes, blobBytes);
  }

  @Test(expected = StorageException.class)
  public void testMetaDataDestruction() throws IOException {
    byte[] preUploadedBytes;
    preUploadedBytes = blobContent.getBytes("UTF-8");
    ionicStorage.create(blobInfo, preUploadedBytes, (BlobTargetOption) null);
    byte[] blobBytes = ionicStorage.readAllBytes(bucketName, blobName);
    String message = "Create and Read Roundtrip Failed";
    assertArrayEquals(message, oriBlobBytes, blobBytes);
    BlobInfo updatedBlobInfo = BlobInfo.newBuilder(blobInfo.getBlobId()).setMetadata(null).build();
    ionicStorage.update(updatedBlobInfo);
    blobBytes = ionicStorage.readAllBytes(bucketName, blobName);
  }

  @Test
  public void testUpdateAgainstMetaCurruption() {
    String badIonicKey = "gibberish";
    HashMap<String, String> blobInfoMetadata = new HashMap<String, String>();
    blobInfoMetadata.put(IONICMETACONSTANT, badIonicKey);

    BlobInfo newBlobInfo =
        BlobInfo.newBuilder(blobInfo.getBlobId()).setMetadata(blobInfoMetadata).build();
    Blob blob = ionicStorage.update(newBlobInfo);
    String ionicKey = blob.getMetadata().get(IONICMETACONSTANT);
    assertFalse(ionicKey == badIonicKey);
  }
}
