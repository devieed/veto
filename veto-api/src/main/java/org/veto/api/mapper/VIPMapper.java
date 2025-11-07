//package org.veto.api.mapper;
//
//import org.mapstruct.Mapper;
//import org.veto.api.vo.VIPInfoVO;
//
//import java.util.List;
//import java.util.function.Function;
//
//@Mapper(componentModel = "spring")
//public interface VIPMapper {
//    default List<VIPInfoVO> toVipVOList(List<LevelProp> levelProps, Integer currentLevel) {
//        return levelProps.stream().map(new Function<LevelProp, VIPInfoVO>() {
//            @Override
//            public VIPInfoVO apply(LevelProp levelProp) {
//                VIPInfoVO vipInfoVO = toVipInfoVO(levelProp);
//                if (currentLevel != null && currentLevel.equals(levelProp.getId())){
//                    vipInfoVO.setCurrentLevel(true);
//                }
//                return vipInfoVO;
//            }
//        }).toList();
//    }
//
//    VIPInfoVO toVipInfoVO(LevelProp levelProp);
//}
