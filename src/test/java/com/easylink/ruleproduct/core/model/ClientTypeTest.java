package com.easylink.ruleproduct.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientTypeTest {

    @Test
    void shouldParseCsvClientTypes() {
        Set<ClientType> types = ClientType.parseCsv("PERSON, ORG,PRODUCT");

        assertThat(types).containsExactlyInAnyOrder(ClientType.PERSON, ClientType.ORG, ClientType.PRODUCT);
    }

    @Test
    void shouldIgnoreBlankAndDashValues() {
        Set<ClientType> types = ClientType.parseCsv("PERSON,-, ");

        assertThat(types).containsExactly(ClientType.PERSON);
    }
}
