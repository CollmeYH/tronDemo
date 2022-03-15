package com.demo.controller;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Contract;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.proto.Chain;
import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;


/**
 * @author wyh
 * @create: 2021-09-25 10:26
 * @Description : 类说明
 */
@Slf4j
public class HelloWorld {


    /**
     * 合约精度
     */
    private final BigDecimal decimal = new BigDecimal("1000000");

    /**
     * 合约地址
     */
    private final String contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    /**
     * 我的地址
     */
    private final String ownerAddr = "TFPodMGEAh6851cUzKeXr9PvuJVfgbbCis";


    @Test
    public void init() {

        ApiWrapper apiWrapper = ApiWrapper.ofMainnet("***7b68641c90175286d8c7f2f36948bc5fef1bed0d463a3429d29fbe835932", "5098c5ed-85b2-4e8f-9a3c-d0ad794c42cb");
        //合约地址
        Contract contract = apiWrapper.getContract(contractAddress);
        Trc20Contract token = new Trc20Contract(contract, ownerAddr, apiWrapper);
        getBalance(token, ownerAddr);

        String txid = token.transfer("TCxPgSxLmpppAL3nToqYZ2d8gGzjy2FC3z", 1, 6, "memo", 10000000);
        log.info("交易id==》{}", txid);

        getTransactionById(apiWrapper, txid);


    }


    /**
     * 获取我的余额
     *
     * @param token
     * @param address
     */
    public void getBalance(Trc20Contract token, String address) {
        BigInteger balance = token.balanceOf(address);
        BigDecimal amount = new BigDecimal(balance).divide(decimal, 6, RoundingMode.FLOOR);
        log.info("获取我的余额==》{}", amount);
    }

    public void getTransactionById(ApiWrapper wrapper, String txid) {

        Chain.Transaction transaction = null;
        try {
            transaction = wrapper.getTransactionById("29168e4ade18ed0973a23c09e61584207db8afa53442eed5f065aa74dbb19a7e");
            Chain.Transaction.Result.code code = transaction.getRet(0).getRet();
            if (code.getNumber() == 0) {
                Chain.Transaction.raw rawData = transaction.getRawData();
                Any parameter = rawData.getContract(0).getParameter();
                ByteString value = parameter.getValue();
                org.tron.trident.proto.Contract.TriggerSmartContract triggerSmartContract = org.tron.trident.proto.Contract.TriggerSmartContract.parseFrom(value);

                ByteString data = triggerSmartContract.getData();
                ByteString contractAddress = triggerSmartContract.getContractAddress();
                ByteString ownerAddress = triggerSmartContract.getOwnerAddress();

                //解析余额
                String s = ApiWrapper.toHex(data.toByteArray());
                BigDecimal amountDecimal = new BigDecimal(new BigInteger(s.substring(72), 16));
                log.info("解析余额：{}", amountDecimal);

                //解析to地址
                String s1 = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(ownerAddress)));

                log.info("解析地址：{}", s1);
            }
        } catch (Exception illegalException) {
            illegalException.printStackTrace();
        }
    }
}


