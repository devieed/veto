package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.web3j.abi.datatypes.Bool;

import java.util.Date;

/**
 * 公告
 */
@Entity
@Table(name = "announcements", indexes = {
        @Index(columnList = "important"),
        @Index(columnList = "created_at"),
        @Index(columnList = "updated_at"),
        @Index(columnList = "status"),
        @Index(columnList = "`type`"),
        @Index(columnList = "is_long")
})
@Setter
@Getter
public class Announcement implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 重要公告
    private Boolean important;

    private String title;

    @Lob
    private String summary;

    @Lob
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String content;
    // 长公告
    private Boolean isLong;

    @Lob
    private String image;

    private Date createdAt;

    private Date updatedAt;

    @Column(name = "`type`")
    @Enumerated(EnumType.ORDINAL)
    private TYPE type;

    private Boolean status;

    @PrePersist
    public void prePersist(){
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = new Date();
    }

    public enum TYPE{
        SYSTEM, // 系统公告
        ACTIVITY, // 活动公告
        MAINTENANCE, // 维护公告
        RULE, // 规则更新
        SECURITY, // 安全提醒
        ;

        public static TYPE me(String str){
            if (str == null){
                return null;
            }
            for (TYPE type : TYPE.values()){
                if (type.name().equalsIgnoreCase(str)){
                    return type;
                }
            }

            return null;
        }
    }
}
