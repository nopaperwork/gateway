/**
 * @package com.nopaper.work.gateway.dto -> gateway
 * @author saikatbarman
 * @date 2025 27-Oct-2025 1:39:51 am
 * @git 
 */
package com.nopaper.work.gateway.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 
 */

public class CustomFilterDTO implements Serializable {
    private static final long serialVersionUID = -1881829638838696562L;
	private String name;
    private Map<String, Object> args;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Object> getArgs() { return args; }
    public void setArgs(Map<String, Object> args) { this.args = args; }
}
