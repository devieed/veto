package org.veto.core.service;

import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.veto.core.rdbms.bean.RecommendRelation;
import org.veto.core.rdbms.bean.RecommendReward;
import org.veto.core.rdbms.repository.RecommendRelationRepository;
import org.veto.core.rdbms.repository.RecommendRewardRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendService {
    @Resource
    private RecommendRelationRepository recommendRelationRepository;

    @Resource
    private RecommendRewardRepository recommendRewardRepository;

    public List<RecommendRelation> getChildNodeTree(Long targetUserId) {
        RecommendReward recommendReward = recommendRewardRepository.findFirstByStatusIsTrueOrderByLevelDesc();

        if (recommendReward == null) {
            return List.of();
        }
        return findAllByTargetUserId(targetUserId, 1, recommendReward.getLevel());
    }

    /**
     * 递归获取推荐关系的子节点
     * @param targetUserId 当前层级的用户ID
     * @param currentDeep 当前深度
     * @param maxDeep 最大递归深度
     * @return 推荐关系列表
     */
    protected List<RecommendRelation> findAllByTargetUserId(Long targetUserId, int currentDeep, int maxDeep) {
        if (currentDeep > maxDeep) {
            // 递归终止条件
            return List.of();
        }

        // 1. 获取当前层级的所有数据，不再进行分页递归
        List<RecommendRelation> res = new ArrayList<>();
        int page = 0;
        Page<RecommendRelation> data;
        do {
            Pageable pageable = PageRequest.of(page, 50);
            data = recommendRelationRepository.findAllByTargetUserIdAndStatusIsTrue(pageable, targetUserId);
            res.addAll(data.getContent());
            page++;
        } while (data.hasNext());

        // 2. 遍历当前层级所有数据，并进行下一层级的递归处理
        res.forEach(r -> {
            r.setDeep(currentDeep);
            r.setChildren(findAllByTargetUserId(r.getTargetUserId(), currentDeep + 1, maxDeep));
        });

        return res;
    }
    // 向上查询自己的父级
    public RecommendRelation getParentTree(Long userId) {
        RecommendReward recommendReward = recommendRewardRepository.findFirstByStatusIsTrueOrderByLevelDesc();

        if (recommendReward == null) {
            return null;
        }

        // 从第一层开始，递归查找父节点
        return findParentNode(userId, 1, recommendReward.getLevel());
    }

    /**
     * 递归获取用户的父级节点树
     * @param userId 当前需要查询父级的用户ID
     * @param currentDeep 当前深度
     * @param maxDeep 最大深度
     * @return 用户的父级节点对象
     */
    protected RecommendRelation findParentNode(Long userId, int currentDeep, int maxDeep) {
        // 递归终止条件1：超过最大深度
        if (currentDeep > maxDeep) {
            return null;
        }

        // 1. 查询当前用户的关系（从子节点ID反查）
        RecommendRelation currentRelation = recommendRelationRepository.findByUserId(userId);

        // 递归终止条件2：没有找到父级关系
        if (currentRelation == null) {
            return null;
        }

        currentRelation.setDeep(currentDeep);

        // 2. 核心修复：用父节点的ID（targetUserId）作为下一次递归的查询ID
        // 否则会无限递归
        RecommendRelation parentRelation = findParentNode(currentRelation.getTargetUserId(), currentDeep + 1, maxDeep);
        if (parentRelation != null) {
            currentRelation.setParent(parentRelation);
        }
        return currentRelation;
    }
}
