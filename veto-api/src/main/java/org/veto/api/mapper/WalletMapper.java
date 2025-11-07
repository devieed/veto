package org.veto.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.veto.api.dto.WithdrawAddDTO;
import org.veto.api.vo.UserWalletVO;
import org.veto.api.vo.WalletRecordVO;
import org.veto.api.vo.WithdrawVO;
import org.veto.core.command.WithdrawAddCommand;
import org.veto.core.rdbms.bean.UserWallet;
import org.veto.core.rdbms.bean.WalletRecord;
import org.veto.core.rdbms.bean.Withdraw;

import java.math.BigDecimal;
@Mapper(componentModel = "spring")
public interface WalletMapper {

    WithdrawAddCommand toWithdrawAddCommand(WithdrawAddDTO withdrawAddDTO);

    @Mapping(target = "coinType", expression = "java(userWallet.getCoinType().name())")
    @Mapping(target = "totalBets", expression = "java(userWallet.getTotalBets())")
    UserWalletVO toUserWalletVO(UserWallet userWallet);

    @Mapping(target = "type", expression = "java(walletRecord.getType().name())")
    WalletRecordVO toWalletRecordVO(WalletRecord walletRecord);

    default Page<WalletRecordVO>  toWalletRecordVOPage(Page<WalletRecord> page) {
        return  page.map(this::toWalletRecordVO);
    }

    default Page<WithdrawVO>  toWithdrawVOPage(Page<Withdraw> page) {
        return page.map(this::toWithdrawVO);
    }

    WithdrawVO toWithdrawVO(Withdraw withdraw);
}
