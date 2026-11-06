package istad.ha.iamservice.infrastructure.security;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.*;
import java.util.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import istad.ha.iamservice.application.AuthUserResult;

/** Internal subject and authorities survive authentication; password credentials are erased. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class IamUserPrincipal implements UserDetails, CredentialsContainer, Serializable {
    private static final long serialVersionUID = 1L;
    @JsonProperty("uuid")
    private final UUID uuid;
    @JsonProperty("username")
    private final String username;
    @JsonIgnore
    private String password;
    @JsonProperty("authorityNames")
    private final Set<String> authorityNames;
    @JsonProperty("accountNonExpired")
    private final boolean accountNonExpired;
    @JsonProperty("accountNonLocked")
    private final boolean accountNonLocked;
    @JsonProperty("credentialsNonExpired")
    private final boolean credentialsNonExpired;
    @JsonProperty("enabled")
    private final boolean enabled;
    private IamUserPrincipal(AuthUserResult result) {
        uuid = result.uuid(); username = result.username(); password = result.passwordHash() == null ? "" : result.passwordHash();
        authorityNames = Collections.unmodifiableSet(new LinkedHashSet<>(result.authorities())); accountNonExpired = result.accountNonExpired();
        accountNonLocked = result.accountNonLocked(); credentialsNonExpired = result.credentialsNonExpired();
        enabled = result.enabled() && result.passwordHash() != null;
    }
    /** JDBC authorizations retain identity/account snapshots, never password credentials. */
    @JsonCreator
    public IamUserPrincipal(@JsonProperty("uuid") UUID uuid, @JsonProperty("username") String username,
            @JsonProperty("authorityNames") Set<String> authorityNames,
            @JsonProperty("accountNonExpired") boolean accountNonExpired,
            @JsonProperty("accountNonLocked") boolean accountNonLocked,
            @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
            @JsonProperty("enabled") boolean enabled) {
        this.uuid = Objects.requireNonNull(uuid); this.username = Objects.requireNonNull(username);
        this.authorityNames = Collections.unmodifiableSet(new LinkedHashSet<>(authorityNames)); this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked; this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }
    public static IamUserPrincipal from(AuthUserResult result) { return new IamUserPrincipal(result); }
    public UUID uuid() { return uuid; }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public void eraseCredentials() { password = null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorityNames.stream().map(SimpleGrantedAuthority::new).toList(); }
    @Override public boolean isAccountNonExpired() { return accountNonExpired; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public String toString() { return "IamUserPrincipal[uuid=" + uuid + ", username=" + username + "]"; }
}
