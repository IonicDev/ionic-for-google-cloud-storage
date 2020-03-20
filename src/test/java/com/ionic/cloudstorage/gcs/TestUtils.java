/*
 * (c) 2019-2020 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.gcs;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.AgentErrorModuleConstants;
import com.ionic.sdk.error.IonicException;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class TestUtils {

    static Logger log = LogManager.getLogger();

    protected static String testBucketEnv = "IONIC_GCS_TEST_BUCKET";
    protected static String testBucketProp = "testBucket";
    protected static String testObjectKeyProp = "objectKey";
    protected static String testPayloadStringProp = "payloadString";
    protected static String testPersistorProp = "persistorPath";

    protected static String defaultPayload = "Hello World.";

    protected static String getTestBucket() {
        String bucket = System.getProperty(testBucketProp);
        if (bucket == null) {
            bucket = System.getenv(testBucketEnv);
        }
        if (bucket == null) {
            log.error("Failed to aquire Bucket from properties and environment");
        }
        return bucket;
    }

    protected static String getTestPayload() {
        String string = System.getProperty(testPayloadStringProp);
        if (string == null) {
            string = defaultPayload;
        }
        return string;
    }

    protected static String getTestObjectKey() {
        return System.getProperty(testObjectKeyProp);
    }

    protected static boolean googleCredentialsAvailable() {
        try {
            GoogleCredential.getApplicationDefault();
        } catch (IOException  e) {
            log.error("Google Credentials Unavailable: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    protected static DeviceProfilePersistorPlainText getPersistor() throws IonicException {
        DeviceProfilePersistorPlainText ptPersistor = null;
        log.info("Attempting to fetch persistor path from properties");
        String ptPersitorPath = System.getProperty(testPersistorProp);
        if (ptPersitorPath == null) {
            log.info("Attempting to load persistor from default location");
            ptPersitorPath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        }
        if (Files.exists(Paths.get(ptPersitorPath))) {
            return new DeviceProfilePersistorPlainText(ptPersitorPath);
        } else {
            log.error("Failed to load persistor from " + ptPersitorPath);
            throw new IonicException(AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE);
        }
    }

    protected static Agent getAgent() throws IonicException {
        log.info("Constructing Ionic Agent with Persisor");
        Agent agent = new Agent();
        agent.initialize(getPersistor());
        return agent;
    }



}
