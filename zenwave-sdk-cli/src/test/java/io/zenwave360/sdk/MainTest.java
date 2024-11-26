package io.zenwave360.sdk;

import org.junit.jupiter.api.Test;

public class MainTest {

    @Test
    void testMain_zdlFiles() {
        String[] args = {"-p", "zdl-to-json", "zdlFiles=classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl,classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl"};
        Main.main(args);
    }
}
