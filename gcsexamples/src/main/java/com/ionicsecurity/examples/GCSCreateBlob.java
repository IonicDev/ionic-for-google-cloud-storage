/*
 * Copyright 2016-2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Obtained from:
 * https://github.com/GoogleCloudPlatform/google-cloud-java/blob/master/google-cloud-examples/src/main/java/com/google/cloud/examples/storage/snippets/CreateBlob.java
 *
 * Modifications Copyright 2018 Ionic Security.
 */

package com.ionicsecurity.examples;

import static java.nio.charset.StandardCharsets.UTF_8;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.util.ArrayList;
import java.util.List;

import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionicsecurity.ipcs.google.GoogleIonicStorage;
import com.ionic.sdk.error.IonicException;

/**
 * A snippet for Google Cloud Storage showing how to create a blob.
 */

public class GCSCreateBlob {

    public static void main(String... args) {
        DeviceProfilePersistorPlainText profilePersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        try {
            profilePersistor.setFilePath(sProfilePath);
        } catch (IonicException e) {
            System.out.println("Error: Can't set the profile persistor path");
            System.exit(-1);
        }

        GoogleIonicStorage ionicStorage;
        try {
            ionicStorage = new GoogleIonicStorage(profilePersistor, StorageOptions.getDefaultInstance().getService());
        } catch (IonicException e) {
            System.out.println("Error: Failed to instantiate an instance of GoogleIonicStorage: " + e.getMessage());
            System.exit(-1);
            return;
        }
        List<String> plaintextAttributeEntry = new ArrayList<String>();
        KeyAttributesMap plaintextAttributes = new KeyAttributesMap();
        plaintextAttributeEntry.add("secret");
        plaintextAttributeEntry.add("keyType1AttributeValue1");
        plaintextAttributes.put("keyType1AttributeField1", plaintextAttributeEntry);
        ionicStorage.setKeyAttributes(plaintextAttributes);

        Storage storage = ionicStorage;
        BlobId blobId = BlobId.of("com-ionic-ipcs-test", "demo_encrypted");
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
        storage.create(blobInfo, "Hello, Cloud Storage!".getBytes(UTF_8));
    }
}