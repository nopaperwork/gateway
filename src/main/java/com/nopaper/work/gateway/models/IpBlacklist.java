/**
 * @package com.nopaper.work.gateway.models -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 1:14:46 am
 * @git 
 */
package com.nopaper.work.gateway.models;

/**
 * 
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entity representing blacklisted IP addresses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ip_blacklist", schema = "way")
public class IpBlacklist {
    
    @Id
    private Long id;
    
    private String ipAddress;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}