/*
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.gcs;

import com.google.api.gax.paging.Page;
import com.google.cloud.Policy;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Entity;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.HmacKey;
import com.google.cloud.storage.ServiceAccount;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageBatch;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.error.IonicException;
import java.io.InputStream;
import java.lang.UnsupportedOperationException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * An Ionic enabled implementation of of the Google {@link com.google.cloud.storage.Storage}
 * interface. create() and writer() methods will generate an Ionic Key and encrypt the blob contents
 * during storage. readAllBytes() and reader() methods will decrypt blob contents locally provided
 * the active profile on the {@link com.ionic.sdk.agent.Agent} has permission to fetch the
 * associated ionicKey.
 */
public class GoogleIonicStorage implements Storage {

    public static final String IONICMETACONSTANT = "ionic-key-id";
    private Storage googleStorage;
    private Agent agent = new Agent();
    private KeyAttributesMap attributes = new KeyAttributesMap();
    private boolean enabledMetadataCapture = false;

    /**
     * GoogleIonicStorage() constructor for GoogleIonicStorage that takes an existing instance of
     * {@link com.google.cloud.storage.Storage} for performing the underlying Storage operations.
     *
     * @param googleStorage an object implimenting {@link com.google.cloud.storage.Storage}.
     */
    public GoogleIonicStorage(Storage googleStorage) {
        this.googleStorage = googleStorage;
    }

    /**
     * GoogleIonicStorage() constructor for GoogleIonicStorage that takes an existing instance of
     * {@link com.google.cloud.storage.Storage} for performing the underlying Storage operations and
     * an Ionic {@link com.ionic.sdk.agent.Agent} for interacting with IDC.
     *
     * @param agent an {@link com.ionic.sdk.agent.Agent} for Ionic api calls.
     * @param googleStorage an object implimenting {@link com.google.cloud.storage.Storage}.
     */
    public GoogleIonicStorage(Agent agent, Storage googleStorage) {
        setAgent(agent);
        this.googleStorage = googleStorage;
    }

    /**
     * GoogleIonicStorage() constructor for GoogleIonicStorage that takes an existing instance of
     * {@link com.google.cloud.storage.Storage} for performing the underlying Storage operations and
     * a persistor.
     * Deprecated. Use {@link #GoogleIonicStorage(Agent, Storage)} instead.
     *
     * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase}
     *     object.
     *
     * @param googleStorage an object implimenting {@link com.google.cloud.storage.Storage}.
     * @throws IonicException if {@link com.ionic.sdk.agent.AgentSdk#initialize()} fails
     */
    @Deprecated
    public GoogleIonicStorage(DeviceProfilePersistorBase persistor, Storage googleStorage)
            throws IonicException {
        setPersistor(persistor);
        this.googleStorage = googleStorage;
    }

    /**
     * setPersistor() sets the Persistor with which to create the Agent instace backing the class.
     * Deprecated. Use {@link #setAgent(Agent)} instead.
     *
     * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase}
     *     object.
     * @throws IonicException on error
     */
    @Deprecated
    public void setPersistor(DeviceProfilePersistorBase persistor) throws IonicException {
        Agent agent = new Agent(persistor);
        this.setAgent(agent);
    }

    /**
     * setAgent() sets the Agent instace backing the class.
     *
     * @param agent an {@link com.ionic.sdk.agent.Agent} object.
     */
    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    /**
     * getAgent() returns the Agent instace backing the class.
     *
     * @return an {@link com.ionic.sdk.agent.Agent} object.
     */
    public Agent getAgent() {
        return this.agent;
    }

    /**
     * setDefaultAttributes() sets the default Attributes to be applied to all Agent.keyCreate()
     * requests
     *
     * @param attributes a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
     */
    public void setDefaultAttributes(KeyAttributesMap attributes) {
        this.attributes = new KeyAttributesMap(attributes);
    }

    /**
     * getDefaultAttributes() gets defaultAttributes
     *
     * @return a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
     */
    public KeyAttributesMap getDefaultAttributes() {
        return new KeyAttributesMap(this.attributes);
    }

    /**
     * isEnabledMetadataCapture() returns enabledMetadataCapture
     *
     * @return a boolean.
     */
    public boolean isEnabledMetadataCapture() {
        return this.enabledMetadataCapture;
    }

    /**
     * setEnabledMetadataCapture() sets enabledMetadataCapture, while true GCS requests to store
     * objects with Encryption will have their Metadata parsed and passed as ionic attributes when
     * content encryption keys are generated.
     *
     * @param enabledMetadataCapture a boolean.
     */
    public void setEnabledMetadataCapture(boolean enabledMetadataCapture) {
        this.enabledMetadataCapture = enabledMetadataCapture;
    }

    /**
     * setIonicMetadataMap() sets the MetadataMap for IDC interactions
     * Deprecated. Use {@link com.ionic.sdk.agent.Agent#setMetadata(MetadataMap)} on the Agent
     * returned by {@link #getAgent()}.
     *
     * @param map a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    @Deprecated
    public void setIonicMetadataMap(MetadataMap map) {
        agent.setMetadata(map);
    }

    /**
     * getIonicMetadataMap() gets the MetadataMap used for IDC interactions
     * Deprecated. Use {@link com.ionic.sdk.agent.Agent#getMetadata()} on the Agent returned by
     * {@link #getAgent()}.
     * @return a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    @Deprecated
    public MetadataMap getIonicMetadataMap() {
        return agent.getMetadata();
    }

    /**
     * Creates a new Ionic protected blob.
     *
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws StorageException upon failure, may wrap an {@link com.ionic.sdk.error.IonicException}
     * @see <a href="https://cloud.google.com/storage/docs/hashes-etags">Hashes and ETags</a>
     */
    @Override
    public Blob create(BlobInfo blobInfo, byte[] content, BlobTargetOption... options) {
        return create(blobInfo, content, new CreateKeysRequest.Key(""), options);
    }

    /**
     * Creates a new Ionic protected blob using a
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key} to specify Attributes on
     * the associated Ionic Key.
     *
     * <p>Example of creating a CreateKeysRequest.Key
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}.
     *
     * <pre>
     * {
     *     &#64;code
     *     KeyAttributesMap attributes = new KeyAttributesMap();
     *     KeyAttributesMap mutableAttributes = new KeyAttributesMap();
     *     attributes.put("Attribute_Key1", Arrays.asList("Val1", "Val2", "Val3"));
     *     mutableAttributes.put("Mutable_Attribute_Key1", Arrays.asList("Val1", "Val2", "Val3"));
     *     CreateKeysRequest.Key reqKey =
     *             new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
     * }
     * </pre>
     *
     * @param blobInfo a {@link com.google.cloud.storage.BlobInfo}
     * @param content a byte[] to store
     * @param key a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a [@code Blob} with complete information
     * @throws StorageException upon failure, may wrap an {@link com.ionic.sdk.error.IonicException}
     * @see #create(BlobInfo, byte[], Storage.BlobTargetOption...)
     * @see <a href="https://cloud.google.com/storage/docs/hashes-etags">Hashes and ETags</a>
     */
    public Blob create(BlobInfo blobInfo, byte[] content, CreateKeysRequest.Key key,
            BlobTargetOption... options) {
        KeyInfoPair pair = null;
        try {
            pair = createIonicKey(key, blobInfo);
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        return googleStorage.create(pair.info, content,
                targetOptionsWithEncrytion(pair.key, options));
    }

    /**
     * Creates a new Ionic protected blob.
     *
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws StorageException upon failure, may wrap an {@link com.ionic.sdk.error.IonicException}
     * @see <a href="https://cloud.google.com/storage/docs/hashes-etags">Hashes and ETags</a>
     */
    @Deprecated
    @Override
    public Blob create(BlobInfo blobInfo, InputStream content, BlobWriteOption... options) {
        return create(blobInfo, content, new CreateKeysRequest.Key(""), options);
    }

    /**
     * Creates a new Ionic protected blob. Deprecated, use
     * {@link #create(BlobInfo, byte[], CreateKeysRequest.Key, Storage.BlobTargetOption...)}
     * instead.
     *
     * @return a [@code Blob} with complete information
     * @param blobInfo a {@link com.google.cloud.storage.BlobInfo}
     * @param content a {@link java.io.InputStream}
     * @param key a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @throws StorageException upon failure, may wrap an {@link com.ionic.sdk.error.IonicException}
     * @see #create(BlobInfo, byte[], CreateKeysRequest.Key, Storage.BlobTargetOption...)
     * @see <a href="https://cloud.google.com/storage/docs/hashes-etags">Hashes and ETags</a>
     */
    @Deprecated
    public Blob create(BlobInfo blobInfo, InputStream content, CreateKeysRequest.Key key,
            BlobWriteOption... options) {
        KeyInfoPair pair = null;
        try {
            pair = createIonicKey(key, blobInfo);
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        return googleStorage.create(pair.info, content,
                writeOptionsWithEncrytion(pair.key, options));
    }

    /**
     * Creates a new Ionic protected blob.
     *
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws StorageException upon failure, may wrap an {@link com.ionic.sdk.error.IonicException}
     * @see <a href="https://cloud.google.com/storage/docs/hashes-etags">Hashes and ETags</a>
     */
    public Blob create(BlobInfo blobInfo, byte[] content, int offset, int length,
            Storage.BlobTargetOption... options) {
        return create(blobInfo, content, offset, length, new CreateKeysRequest.Key(""), options);
    }


    /**
     * Creates a new Ionic protected blob using a
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key} to specify Attributes on
     * the associated Ionic Key.
     *
     * <p>Example of creating a CreateKeysRequest.Key
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}.
     *
     * <pre>
     * {
     *     &#64;code
     *     KeyAttributesMap attributes = new KeyAttributesMap();
     *     KeyAttributesMap mutableAttributes = new KeyAttributesMap();
     *     attributes.put("Attribute_Key1", Arrays.asList("Val1", "Val2", "Val3"));
     *     mutableAttributes.put("Mutable_Attribute_Key1", Arrays.asList("Val1", "Val2", "Val3"));
     *     CreateKeysRequest.Key reqKey =
     *             new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
     * }
     * </pre>
     *
     * @param blobInfo a {@link com.google.cloud.storage.BlobInfo}
     * @param content a byte[] to store
     * @param offset number of bytes off from the start of content to upload
     * @param length total number of bytes from content to upload
     * @param key a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a [@code Blob} with complete information
     * @throws StorageException upon failure, may wrap an {@link com.ionic.sdk.error.IonicException}
     * @see #create(BlobInfo, byte[], Storage.BlobTargetOption...)
     * @see <a href="https://cloud.google.com/storage/docs/hashes-etags">Hashes and ETags</a>
     */
    public Blob create(BlobInfo blobInfo, byte[] content, int offset, int length,
            CreateKeysRequest.Key key, Storage.BlobTargetOption... options) {
        byte[] subContent = Arrays.copyOfRange(content, offset, offset + length);
        return create(blobInfo, subContent, key, options);
    }

    /**
     * {@inheritDoc} Warning: Clearing an ecrypted blob's metadata or modifying the value of the
     * 'ionic-key-id' entry in the blob's metadata map will cause the blob contents to be
     * unrecoverable.
     */
    @Override
    public Blob update(BlobInfo blobInfo, BlobTargetOption... options) {
        return googleStorage.update(blobInfo, options);
    }

    /**
     * {@inheritDoc} Warning: Clearing an ecrypted blob's metadata or modifying the value of the
     * 'ionic-key-id' entry in the blob's metadata map will cause the blob contents to be
     * unrecoverable.
     */
    @Override
    public Blob update(BlobInfo blobInfo) {
        return googleStorage.update(blobInfo, new BlobTargetOption[0]);
    }

    /**
     * A container class that holds a pairing of
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key} and a byte[] returned by
     * readAllBytesAndKey() methods.
     */
    public class IonicKeyBytesPair {
        private GetKeysResponse.Key key;
        private byte[] byteArray;

        private IonicKeyBytesPair(GetKeysResponse.Key key, byte[] byteArray) {
            this.key = key;
            this.byteArray = byteArray;
        }

        /**
         * Returns a GetKeysResponse.Key.
         *
         * @return a {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}
         */
        public GetKeysResponse.Key getKey() {
            return this.key;
        }

        /**
         * Returns a byte[].
         *
         * @return a byte[]
         */
        public byte[] getByteArray() {
            return Arrays.copyOf(this.byteArray, this.byteArray.length);
        }
    }

    /**
     * Reads all the bytes from an Ionic protected blob. Will throw a StorageException if the
     * {@link com.ionic.sdk.agent.Agent Agent's} active profile does not have permission to
     * fetch the associated ionicKey.
     *
     * @return the blob's content
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#readAllBytes(String, String, BlobSourceOption...)
     */
    @Override
    public byte[] readAllBytes(String bucketName, String blobName, BlobSourceOption... options) {
        return readAllBytesAndKey(BlobId.of(bucketName, blobName), options).getByteArray();
    }

    /**
     * Reads all the bytes from an Ionic protected blob and returns a IonicKeyBytesPair containing
     * the byte[] and the {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}. Will
     * throw a StorageException if the the loaded
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does not
     * have permission to fetch the associated ionicKey.
     *
     * @param bucketName the bucket to store the blob in
     * @param blobName the name for the blob to be stored as
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a {@link IonicKeyBytesPair}
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#readAllBytes(String, String, BlobSourceOption...)
     * @see #readAllBytes(String, String, Storage.BlobSourceOption...)
     */
    public IonicKeyBytesPair readAllBytesAndKey(String bucketName, String blobName,
            BlobSourceOption... options) {
        return readAllBytesAndKey(BlobId.of(bucketName, blobName), options);
    }

    /**
     * Reads all the bytes from an Ionic protected blob. Will throw a StorageException if the the
     * loaded {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does
     * not have permission to fetch the associated ionicKey.
     *
     * @return the blob's content
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#readAllBytes(BlobId, BlobSourceOption...)
     */
    @Override
    public byte[] readAllBytes(BlobId blobId, BlobSourceOption... options) {
        return readAllBytesAndKey(blobId, options).getByteArray();
    }

    /**
     * Reads all the bytes from an Ionic protected blob and returns a IonicKeyBytesPair containing
     * the byte[] and the {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}. Will
     * throw a StorageException if the the loaded
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does not
     * have permission to fetch the associated ionicKey.
     *
     * @param blobId the {@link com.google.cloud.storage.BlobId} to be stored
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a {@link IonicKeyBytesPair}
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#readAllBytes(BlobId, BlobSourceOption...)
     * @see #readAllBytes(BlobId, Storage.BlobSourceOption...)
     */
    public IonicKeyBytesPair readAllBytesAndKey(BlobId blobId, BlobSourceOption... options) {
        GetKeysResponse.Key ionicKey = ionicKeyFromBlob(blobId);
        byte[] bytes =
                googleStorage.readAllBytes(blobId, sourceOptionsWithDecryption(ionicKey, options));
        return new IonicKeyBytesPair(ionicKey, bytes);
    }

    /**
     * A container class that holds a pairing of
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key} and a
     * {@link com.google.cloud.ReadChannel} returned by readAllBytesAndKey() methods.
     */
    public class IonicKeyReadChannelPair {
        private GetKeysResponse.Key key;
        private ReadChannel reader;

        private IonicKeyReadChannelPair(GetKeysResponse.Key key, ReadChannel reader) {
            this.key = key;
            this.reader = reader;
        }

        /**
         * Returns a GetKeysResponse.Key.
         *
         * @return a {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}
         */
        public GetKeysResponse.Key getKey() {
            return this.key;
        }

        /**
         * Returns a ReadChannel.
         *
         * @return a {@link com.google.cloud.ReadChannel}
         */
        public ReadChannel getReadChannel() {
            return this.reader;
        }
    }

    /**
     * {@inheritDoc} Will throw a StorageException if the the loaded
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does not
     * have permission to fetch the associated ionicKey or if the blob is not Ionic protected.
     *
     * @return a {@link com.google.cloud.ReadChannel}
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#reader(String, String, BlobSourceOption...)
     */
    @Override
    public ReadChannel reader(String bucket, String blob, BlobSourceOption... options) {
        return readerAndKey(BlobId.of(bucket, blob), options).getReadChannel();
    }

    /**
     * Returns a IonicKeyReadChannelPair containing a
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key} and a channel for
     * reading the blob's content. The blob's latest generation is read. If the blob changes while
     * reading (i.e. {@link com.google.cloud.storage.BlobInfo#getEtag()} changes), subsequent calls
     * to {@code blobReadChannel.read(ByteBuffer)} may throw {@link StorageException}. Will throw a
     * StorageException if the the loaded
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does not
     * have permission to fetch the associated ionicKey or if the blob is not Ionic protected.
     *
     * @param bucket the bucket holding the blob
     * @param blob the name of the blob to be read
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a {@link IonicKeyReadChannelPair}
     * @throws StorageException upon failure
     * @see #reader(String, String, Storage.BlobSourceOption...)
     * @see com.google.cloud.storage.Storage#reader(String, String, BlobSourceOption...)
     */
    public IonicKeyReadChannelPair readerAndKey(String bucket, String blob,
            BlobSourceOption... options) {
        return readerAndKey(BlobId.of(bucket, blob), options);
    }

    /**
     * {@inheritDoc} Will throw a StorageException if the the loaded
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does not
     * have permission to fetch the associated ionicKey or if the blob is not Ionic protected.
     *
     * @return a {@link com.google.cloud.ReadChannel}
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#reader(BlobId, BlobSourceOption...)
     */
    @Override
    public ReadChannel reader(BlobId blob, BlobSourceOption... options) {
        return readerAndKey(blob, options).getReadChannel();
    }

    /**
     * Returns a IonicKeyReadChannelPair containing a
     * {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key} and a channel for
     * reading the blob's content. If {@code blob.generation()} is set data corresponding to that
     * generation is read. The blob's latest generation is read. If the blob changes while reading
     * (i.e. {@link com.google.cloud.storage.BlobInfo#getEtag()} changes), subsequent calls to
     * {@code blobReadChannel.read(ByteBuffer)} may throw {@link StorageException}. Will throw a
     * StorageException if the the loaded
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Profile} does not
     * have permission to fetch the associated ionicKey or if the blob is not Ionic protected.
     *
     * <p>The {@link BlobSourceOption#generationMatch()} and
     * {@link BlobSourceOption#generationMatch(long)} options can be used to ensure that
     * {@code blobReadChannel.read(ByteBuffer)} calls will throw {@link StorageException} if the
     * blob`s generation differs from the expected one.
     *
     * @param blob the {@link com.google.cloud.storage.BlobId} of the blob to be read
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a {@link IonicKeyReadChannelPair}
     * @throws StorageException upon failure
     * @see #reader(BlobId, Storage.BlobSourceOption...)
     * @see com.google.cloud.storage.Storage#reader(BlobId, BlobSourceOption...)
     */
    public IonicKeyReadChannelPair readerAndKey(BlobId blob, BlobSourceOption... options) {
        GetKeysResponse.Key ionicKey = ionicKeyFromBlob(blob);
        ReadChannel reader =
                googleStorage.reader(blob, sourceOptionsWithDecryption(ionicKey, options));
        return new IonicKeyReadChannelPair(ionicKey, reader);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link com.google.cloud.WriteChannel}
     * @throws StorageException upon failure
     * @see com.google.cloud.storage.Storage#writer(BlobInfo, BlobWriteOption...)
     */
    @Override
    public WriteChannel writer(BlobInfo blobInfo, BlobWriteOption... options) {
        return writer(blobInfo, new CreateKeysRequest.Key(""), options);
    }

    /**
     * Creates an Ionic protected blob and return a channel for writing its content. By default any
     * md5 and crc32c values in the given {@code blobInfo} are ignored unless requested via the
     * {@code BlobWriteOption.md5Match} and {@code BlobWriteOption.crc32cMatch} options.
     *
     * <p>Example of writing a blob's content through a writer.
     *
     * <pre>
     * {
     *     &#64;code
     *     String bucketName = "my_unique_bucket";
     *     String blobName = "my_blob_name";
     *     BlobId blobId = BlobId.of(bucketName, blobName);
     *     byte[] content = "Hello, World!".getBytes(UTF_8);
     *     KeyAttributesMap attributes = new KeyAttributesMap();
     *     KeyAttributesMap mutableAttributes = new KeyAttributesMap();
     *     attributes.put("Attribute_Key1", Arrays.asList("Val1", "Val2", "Val3"));
     *     mutableAttributes.put("Mutable_Attribute_Key1", Arrays.asList("Val1", "Val2", "Val3"));
     *     CreateKeysRequest.Key reqKey =
     *             new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
     *     BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
     *     try (WriteChannel writer = storage.writer(blobInfo, reqKey)) {
     *         try {
     *             writer.write(ByteBuffer.wrap(content, 0, content.length));
     *         } catch (Exception ex) {
     *             // handle exception
     *         }
     *     }
     * }
     * </pre>
     *
     * @param blobInfo the {@link com.google.cloud.storage.BlobInfo} to be stored
     * @param key a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * @param options an optional array of
     *        {@link com.google.cloud.storage.Storage.BlobTargetOption}s
     * @return a {@link com.google.cloud.WriteChannel}
     * @throws StorageException upon failure
     * @see #writer(BlobInfo, Storage.BlobWriteOption...)
     * @see com.google.cloud.storage.Storage#writer(BlobInfo, BlobWriteOption...)
     */
    public WriteChannel writer(BlobInfo blobInfo, CreateKeysRequest.Key key,
            BlobWriteOption... options) {
        KeyInfoPair pair = null;
        try {
            pair = createIonicKey(key, blobInfo);
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        return googleStorage.writer(pair.info, writeOptionsWithEncrytion(pair.key, options));
    }


    /**
     * This method is not currently supported by the GoogleIonicStorage class and will always throw
     * an exception.
     *
     * @param signedURL a presigned url to upload to.
     * @return a {@link com.google.cloud.WriteChannel}
     * @throws StorageException Always
     */
    public WriteChannel writer(URL signedURL) {
        String message = "Unsupported Method";
        throw new StorageException(0, message, new UnsupportedOperationException(message));
    }

    // Internal methods

    protected GetKeysResponse.Key ionicKeyFromBlob(BlobId blobId) {
        Blob sourceBlob = googleStorage.get(blobId, BlobGetOption.fields(BlobField.METADATA));
        if (sourceBlob == null) {
            throw new StorageException(404, "404 Not Found");
        }
        try {
            return getIonicKey(sourceBlob.getMetadata().get(IONICMETACONSTANT));
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        } catch (NullPointerException e) {
            return null; // Ionic Key not present in blob metadata.
        }
    }

    protected class KeyInfoPair {
        protected CreateKeysResponse.Key key;
        protected BlobInfo info;

        KeyInfoPair(CreateKeysResponse.Key key, BlobInfo info) {
            this.key = key;
            this.info = info;
        }
    }

    protected KeyInfoPair createIonicKey(CreateKeysRequest.Key key, BlobInfo blobInfoIn)
            throws IonicException {
        KeyAttributesMap attributesMap = new KeyAttributesMap();
        Map<String, String> blobInfoInMetadata = blobInfoIn.getMetadata();
        HashMap<String, String> blobInfoOutMetadata;
        if (blobInfoInMetadata != null) {
            blobInfoOutMetadata = new HashMap<String, String>(blobInfoInMetadata);
            if (enabledMetadataCapture) {
                for (Map.Entry<String, String> entry : blobInfoOutMetadata.entrySet()) {
                    ArrayList<String> collection = new ArrayList<String>();
                    collection.add(entry.getValue());
                    attributesMap.put(entry.getKey(), collection);
                }
            }
        } else {
            blobInfoOutMetadata = new HashMap<String, String>();
        }
        attributesMap.putAll(attributes);
        attributesMap.putAll(key.getAttributesMap());
        Agent agent = Agent.clone(this.agent);
        CreateKeysResponse.Key ionicKey =
                agent.createKey(attributesMap, key.getMutableAttributesMap()).getFirstKey();
        blobInfoOutMetadata.put(IONICMETACONSTANT, ionicKey.getId());
        BlobInfo blobInfoOut = blobInfoIn.toBuilder().setMetadata(blobInfoOutMetadata).build();
        return new KeyInfoPair(ionicKey, blobInfoOut);
    }

    protected GetKeysResponse.Key getIonicKey(String keyid) throws IonicException {
        return Agent.clone(this.agent).getKey(keyid).getFirstKey();
    }

    protected BlobWriteOption[] writeOptionsWithEncrytion(CreateKeysResponse.Key ionicKey,
            BlobWriteOption... options) {
        BlobWriteOption[] newOptions;
        BlobWriteOption encBtg = BlobWriteOption.encryptionKey(ionicKey.getSecretKey());
        if (options == null || options.length == 0 || options[0] == null) {
            newOptions = new BlobWriteOption[1];
        } else {
            newOptions = new BlobWriteOption[options.length + 1];
            System.arraycopy(options, 0, newOptions, 1, options.length);
        }
        newOptions[0] = encBtg;
        return newOptions;
    }

    protected BlobTargetOption[] targetOptionsWithEncrytion(CreateKeysResponse.Key ionicKey,
            BlobTargetOption... options) {
        BlobTargetOption[] newOptions;
        BlobTargetOption encBtg = BlobTargetOption.encryptionKey(ionicKey.getSecretKey());
        if (options == null || options.length == 0 || options[0] == null) {
            newOptions = new BlobTargetOption[1];
        } else {
            newOptions = new BlobTargetOption[options.length + 1];
            System.arraycopy(options, 0, newOptions, 1, options.length);
        }
        newOptions[0] = encBtg;
        return newOptions;
    }

    protected BlobSourceOption[] sourceOptionsWithDecryption(GetKeysResponse.Key ionicKey,
            BlobSourceOption... options) {
        if (ionicKey == null) {
            return options;
        }
        BlobSourceOption[] newOptions;
        BlobSourceOption encBtg = BlobSourceOption.decryptionKey(ionicKey.getSecretKey());
        if (options == null || options.length == 0 || options[0] == null) {
            newOptions = new BlobSourceOption[1];
        } else {
            newOptions = new BlobSourceOption[options.length + 1];
            System.arraycopy(options, 0, newOptions, 1, options.length);
        }
        newOptions[0] = encBtg;
        return newOptions;
    }

    // Unaltered Storage methods.

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Bucket lockRetentionPolicy(BucketInfo bucket, BucketTargetOption... options) {
        return googleStorage.lockRetentionPolicy(bucket, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Blob create(BlobInfo blobInfo, BlobTargetOption... options) {
        return googleStorage.create(blobInfo, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public StorageOptions getOptions() {
        return googleStorage.getOptions();
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Bucket create(BucketInfo bucketInfo, BucketTargetOption... options) {
        return googleStorage.create(bucketInfo, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public HmacKey createHmacKey(ServiceAccount serviceAccount,
            Storage.CreateHmacKeyOption... options) {
        return googleStorage.createHmacKey(serviceAccount, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public void deleteHmacKey(HmacKey.HmacKeyMetadata hmacKeyMetadata,
            Storage.DeleteHmacKeyOption... options) {
        googleStorage.deleteHmacKey(hmacKeyMetadata, options);
        return;
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public HmacKey.HmacKeyMetadata getHmacKey(String accessId,
            Storage.GetHmacKeyOption... options) {
        return googleStorage.getHmacKey(accessId, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public com.google.api.gax.paging.Page<HmacKey.HmacKeyMetadata> listHmacKeys(
            Storage.ListHmacKeysOption... options) {
        return googleStorage.listHmacKeys(options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public HmacKey.HmacKeyMetadata updateHmacKeyState(HmacKey.HmacKeyMetadata hmacKeyMetadata,
            HmacKey.HmacKeyState state, Storage.UpdateHmacKeyOption... options) {
        return googleStorage.updateHmacKeyState(hmacKeyMetadata, state, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Bucket get(String bucket, BucketGetOption... options) {
        return googleStorage.get(bucket, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Blob get(String bucket, String blob, BlobGetOption... options) {
        return googleStorage.get(bucket, blob, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Blob get(BlobId blob, BlobGetOption... options) {
        return googleStorage.get(blob, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Blob get(BlobId blob) {
        return googleStorage.get(blob);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Page<Bucket> list(BucketListOption... options) {
        return googleStorage.list(options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Page<Blob> list(String bucket, BlobListOption... options) {
        return googleStorage.list(bucket, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Bucket update(BucketInfo bucketInfo, BucketTargetOption... options) {
        return googleStorage.update(bucketInfo, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean delete(String bucket, BucketSourceOption... options) {
        return googleStorage.delete(bucket, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean delete(String bucket, String blob, BlobSourceOption... options) {
        return googleStorage.delete(bucket, blob, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean delete(BlobId blob, BlobSourceOption... options) {
        return googleStorage.delete(blob, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean delete(BlobId blob) {
        return googleStorage.delete(blob);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Blob compose(ComposeRequest composeRequest) {
        return googleStorage.compose(composeRequest);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public CopyWriter copy(CopyRequest copyRequest) {
        return googleStorage.copy(copyRequest);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public StorageBatch batch() {
        return googleStorage.batch();
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public URL signUrl(BlobInfo blobInfo, long duration, TimeUnit unit, SignUrlOption... options) {
        return googleStorage.signUrl(blobInfo, duration, unit, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Blob> get(BlobId... blobIds) {
        return googleStorage.get(blobIds);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Blob> get(Iterable<BlobId> blobIds) {
        return googleStorage.get(blobIds);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Blob> update(BlobInfo... blobInfos) {
        return googleStorage.update(blobInfos);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Blob> update(Iterable<BlobInfo> blobInfos) {
        return googleStorage.update(blobInfos);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Boolean> delete(BlobId... blobIds) {
        return googleStorage.delete(blobIds);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Boolean> delete(Iterable<BlobId> blobIds) {
        return googleStorage.delete(blobIds);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl getAcl(String bucket, Entity entity) {
        return googleStorage.getAcl(bucket, entity);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean deleteAcl(String bucket, Entity entity) {
        return googleStorage.deleteAcl(bucket, entity);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl createAcl(String bucket, Acl acl) {
        return googleStorage.createAcl(bucket, acl);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl updateAcl(String bucket, Acl acl) {
        return googleStorage.createAcl(bucket, acl);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Acl> listAcls(String bucket) {
        return googleStorage.listAcls(bucket);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl getDefaultAcl(String bucket, Entity entity) {
        return googleStorage.getDefaultAcl(bucket, entity);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean deleteDefaultAcl(String bucket, Entity entity) {
        return googleStorage.deleteDefaultAcl(bucket, entity);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl createDefaultAcl(String bucket, Acl acl) {
        return googleStorage.createDefaultAcl(bucket, acl);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl updateDefaultAcl(String bucket, Acl acl) {
        return googleStorage.updateDefaultAcl(bucket, acl);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Acl> listDefaultAcls(String bucket) {
        return googleStorage.listDefaultAcls(bucket);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl getAcl(String bucket, Entity entity, BucketSourceOption... options) {
        return googleStorage.getAcl(bucket, entity, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl getAcl(BlobId blob, Entity entity) {
        return googleStorage.getAcl(blob, entity);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean deleteAcl(String bucket, Entity entity, BucketSourceOption... options) {
        return googleStorage.deleteAcl(bucket, entity, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public boolean deleteAcl(BlobId blob, Entity entity) {
        return googleStorage.deleteAcl(blob, entity);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl createAcl(String bucket, Acl acl, BucketSourceOption... options) {
        return googleStorage.createAcl(bucket, acl, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl createAcl(BlobId blob, Acl acl) {
        return googleStorage.createAcl(blob, acl);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl updateAcl(String bucket, Acl acl, BucketSourceOption... options) {
        return googleStorage.updateAcl(bucket, acl, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Acl updateAcl(BlobId blob, Acl acl) {
        return googleStorage.updateAcl(blob, acl);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Acl> listAcls(String bucket, BucketSourceOption... options) {
        return googleStorage.listAcls(bucket, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Acl> listAcls(BlobId blob) {
        return googleStorage.listAcls(blob);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Policy getIamPolicy(String bucket, BucketSourceOption... options) {
        return googleStorage.getIamPolicy(bucket, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public Policy setIamPolicy(String bucket, Policy policy, BucketSourceOption... options) {
        return googleStorage.setIamPolicy(bucket, policy, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public List<Boolean> testIamPermissions(String bucket, List<String> permissions,
            BucketSourceOption... options) {
        return googleStorage.testIamPermissions(bucket, permissions, options);
    }

    /**@deprecated passthrough method. Don't document.*/
    @Override
    public ServiceAccount getServiceAccount(String projectId) {
        return googleStorage.getServiceAccount(projectId);
    }
}
