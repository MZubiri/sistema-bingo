package com.bingo.backend.vendor;

import com.bingo.backend.common.OrganizationCode;

public final class VendorDtos {
    private VendorDtos() {
    }

    public record VendorDashboardResponse(
            String fullName,
            OrganizationCode organizationCode,
            long totalInOrganization,
            long assignedInOrganization,
            long availableInOrganization
    ) {
    }
}
