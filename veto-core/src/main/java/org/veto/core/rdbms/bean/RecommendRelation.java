package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import java.util.Date;
import java.util.List;

@Table(name = "recommend_relations", indexes = {
    @Index(columnList = "user_id", unique = true),
    @Index(columnList = "target_user_id"),
})
@Entity
@Setter
@Getter
public class RecommendRelation implements VetoBean{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    private Date createdAt;

    private Boolean status;

    @Transient
    private Integer deep;

    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
    }


    @PreUpdate
    public void preUpdate() {
        this.createdAt = new Date();
    }

    @Transient
    private List<RecommendRelation> children;

    @Transient
    private RecommendRelation parent;
//
//    // OneToMany 关联：一个父节点（user_id）可以有多个子节点（target_user_id）
//    // name: 外键列名（在子节点表里），referencedColumnName: 主键列名（在当前表里）
//    @OneToMany(fetch = FetchType.LAZY)
//    @JoinColumn(name = "target_user_id", referencedColumnName = "user_id", updatable = false, insertable = false)
//    private List<RecommendRelation> children;
//
//    // ManyToOne 关联：一个子节点（user_id）只有一个父节点（target_user_id）
//    // name: 外键列名（在当前表里），referencedColumnName: 主键列名（在父节点表里）
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "target_user_id", referencedColumnName = "user_id", updatable = false, insertable = false)
//    private RecommendRelation parent;
}
