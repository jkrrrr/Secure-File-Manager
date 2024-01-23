import org.SFM.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class MainTest {
    private CryptoHandler ch;
    private DirectoryHandler dh;
    private PasswordHandler ph;
    private AccessHandler ah;

    @Before
    public void setup() throws Exception {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("config.properties");
        properties.load(input);

        String baseDir = properties.getProperty("baseDir");
        String passwordPath = properties.getProperty("passwordPath");

        // Load classes
        this.ch = CryptoHandler.getInstance();
        this.dh = new DirectoryHandler(baseDir);
        this.ph = PasswordHandler.getInstance();
        this.ph.setPath(passwordPath);
        this.ah = new AccessHandler();

        String username = "user a";
        String password = "user a";

        ah.createUser(username, password);


    }

    @Test
    public void test_createUser(){
        String username = RandomStringUtils.random(10, true, true);
        String password = RandomStringUtils.random(10, true, true);

        boolean result = ah.createUser(username, password);

        assertTrue(result);

    }

    @Test
    public void test_authenticateUser() throws Exception {
        boolean result = ah.authenticate("user a", "user a");

        assertTrue(result);
    }

    @Test
    public void test_authenticateUserMulti() throws Exception {
        String username = RandomStringUtils.random(10, true, true);
        String password = RandomStringUtils.random(10, true, true);

        ah.createUser(username, password);

        boolean result = ah.authenticate(username, password);

        assertTrue(result);
    }
}
