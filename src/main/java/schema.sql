-- Gateway Routes Configuration
CREATE TABLE IF NOT EXISTS gateway_routes (
    id SERIAL PRIMARY KEY,
    route_id VARCHAR(100) UNIQUE NOT NULL,
    uri VARCHAR(500) NOT NULL,
    path VARCHAR(255) NOT NULL,
    method VARCHAR(10),
    enabled BOOLEAN DEFAULT true,
    rate_limit_requests INTEGER DEFAULT 100,
    rate_limit_period_seconds INTEGER DEFAULT 60,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- IP Blacklist
CREATE TABLE IF NOT EXISTS ip_blacklist (
    id SERIAL PRIMARY KEY,
    ip_address VARCHAR(45) UNIQUE NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Request Audit Log
CREATE TABLE IF NOT EXISTS request_audit_log (
    id SERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL,
    route_id VARCHAR(100),
    method VARCHAR(10) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    query_params TEXT,
    client_ip VARCHAR(45),
    user_agent VARCHAR(1000),
    request_headers TEXT,
    request_body TEXT,
    response_status INTEGER,
    response_headers TEXT,
    response_body TEXT,
    processing_time_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_routes_enabled ON gateway_routes(enabled);
CREATE INDEX idx_blacklist_ip ON ip_blacklist(ip_address);
CREATE INDEX idx_audit_created ON request_audit_log(created_at DESC);
CREATE INDEX idx_audit_route ON request_audit_log(route_id);

-- Sample Routes Data
INSERT INTO gateway_routes (route_id, uri, path, method, rate_limit_requests, rate_limit_period_seconds, description) 
VALUES 
('user-service', 'http://localhost:8081', '/api/users/**', 'GET', 100, 60, 'User Management Service'),
('order-service', 'http://localhost:8082', '/api/orders/**', NULL, 50, 60, 'Order Management Service'),
('product-service', 'http://localhost:8083', '/api/products/**', 'GET', 200, 60, 'Product Catalog Service')
ON CONFLICT (route_id) DO NOTHING;

-- Sample Blacklist Data
INSERT INTO ip_blacklist (ip_address, reason, expires_at) 
VALUES 
('192.168.1.100', 'Suspicious activity detected', NOW() + INTERVAL '7 days')
ON CONFLICT (ip_address) DO NOTHING;
