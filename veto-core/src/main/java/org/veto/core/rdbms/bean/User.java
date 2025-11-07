package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "users", indexes = {
        @Index(columnList = "username", unique = true),
        @Index(columnList = "username,password"),
        @Index(columnList = "status"),
        @Index(columnList = "created_at"),
        @Index(columnList = "updated_at"),
        @Index(columnList = "last_login_at"),
        @Index(columnList = "recommend_code", unique = true),
        @Index(columnList = "nickname", unique = true),
        @Index(columnList = "phone", unique = true),
        @Index(columnList = "email", unique = true),
        @Index(columnList = "recommend_user_count"),
        @Index(columnList = "is_vip")
})
@Setter
@Getter
public class User implements VetoBean {
    @Id
    private Long id;

    private String username;

    private String nickname;

    private String password;
    // TODO: support other status, eq: not verify email or not set nickname....
    private Boolean status;

    private Date createdAt;

    private Date updatedAt;

    private Date lastLoginAt;

    private String recommendCode;

    private String phone;

    private String realName;

    private String email;

    @JoinColumn(name = "id", referencedColumnName = "user_id", insertable = false, updatable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private RealNameAuthentication realNameAuthentication;

    @Lob
    private String avatar;

    private Integer recommendUserCount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "user_id", updatable = false, insertable = false)
    private UserWallet userWallet;

    @Transient
    private Date tokenExpireAt;

    @Transient
    private String token;

    private Boolean isVip;

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = new Date();
    }
}
