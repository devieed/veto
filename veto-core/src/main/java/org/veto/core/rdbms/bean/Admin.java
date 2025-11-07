package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.util.Date;

@Entity
@Table(name = "admins", indexes = {
        @Index(columnList = "username", unique = true),
        @Index(columnList = "username,password"),
        @Index(columnList = "last_login")
})
@Setter
@Getter
public class Admin implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    private String password;

    private Date lastLogin;

    private Boolean status;
}
