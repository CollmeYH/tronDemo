package com.demo.controller;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.trident.abi.TypeDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
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
 * @create: 2021-11-11 10:57
 * @Description : TRC20交互
 */
@Slf4j
@Component
public class TronUtils {

	@Value("${trx.privateKey}")
	private String privateKey;

	@Value("${trx.apiKey}")
	private String apiKey;

	@Value("${trx.contractAddress}")
	private String contractAddress;


	@Value("${trx.ownerAddress}")
	private String ownerAddress;

	@Value("${trx.feeLimit}")
	private Long feeLimit;


	/**
	 * 合约精度
	 */
	private final BigDecimal decimal = new BigDecimal("1000000");


	/**
	 * TRC20转账
	 *
	 * @param toAddress to地址
	 * @param amount    金额
	 * @return txid
	 */
	public String sendTransaction(String toAddress, BigDecimal amount) {
		ApiWrapper apiWrapper = ApiWrapper.ofMainnet(privateKey, apiKey);
		//去除无效0
		String s = amount.stripTrailingZeros().toPlainString();
		amount = new BigDecimal(s);

		//合约地址
		Contract contract = apiWrapper.getContract(contractAddress);
		Trc20Contract token = new Trc20Contract(contract, ownerAddress, apiWrapper);


		int scale = amount.scale();
		int price = amount.multiply(getNum(scale)).intValue();

		log.info("发起交易==》toAddress:{},price:{},scale:{}", toAddress, price, scale);
		//处理精度
		String txid = token.transfer(toAddress, price, 6 - scale, "memo", feeLimit);
		log.info("交易id==》{}", txid);
		apiWrapper.close();
		return txid;
	}


	/**
	 * 根据交易id查询TRC20交易详情
	 *
	 * @param txid hash
	 * @return 详情
	 */
	public TronTransaction getTransaction(String txid) {
		TronTransaction tron = new TronTransaction();
		ApiWrapper apiWrapper = null;
		try {
			apiWrapper = ApiWrapper.ofMainnet(privateKey, apiKey);
			Chain.Transaction transaction = null;

			//以下操作是防止签名交易了，但查询太快导致订单不存在
			Boolean flag = true;
			for (int i = 0; i < 10; i++) {
				if (flag) {
					try {
						transaction = apiWrapper.getTransactionById(txid);
						Chain.Transaction.Result result = transaction.getRet(0);
						String w = result.getContractRet().toString();
						log.info("交易状态==》{}", w);
						if (w.equals("SUCCESS")){
							flag = false;
						}
					} catch (IllegalException e) {
						log.error("查询订单失败，第【{}】次查询，错误信息:{}", i + 1, e.toString());
						flag = true;
					}
					Thread.sleep(1000L);
				}
			}

			if (transaction == null) {
				tron.setCode(false);
				return tron;
			}
			Chain.Transaction.Result result = transaction.getRet(0);
			String w = result.getContractRet().toString();
			if ("SUCCESS".equals(w)) {
				Chain.Transaction.raw rawData = transaction.getRawData();
				Any parameter = rawData.getContract(0).getParameter();
				ByteString value = parameter.getValue();
				org.tron.trident.proto.Contract.TriggerSmartContract triggerSmartContract = org.tron.trident.proto.Contract.TriggerSmartContract.parseFrom(value);

				ByteString data = triggerSmartContract.getData();
				ByteString contractAddress = triggerSmartContract.getContractAddress();
				ByteString ownerAddress = triggerSmartContract.getOwnerAddress();
				//解析余额
				String s = ApiWrapper.toHex(data.toByteArray());
				TypeReference<Address> addressTypeReference = TypeReference.create(Address.class);
				Address toAdress = TypeDecoder.decodeStaticStruct(s.substring(8, 72), 0, addressTypeReference);
				BigDecimal amountDecimal = new BigDecimal(new BigInteger(s.substring(72), 16));

				//解析to地址
				String s1 = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(ownerAddress)));
				String s2 = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(contractAddress)));

				tron.setCode(true);
				tron.setOwnerAddress(s1);
				tron.setContractAddress(s2);
				tron.setAmount(amountDecimal.divide(decimal, 4, RoundingMode.DOWN));

				log.info("解析余额====》{}", amountDecimal.divide(decimal, 4, RoundingMode.DOWN));
				log.info("解析地址from====》{}", s1);
				log.info("解析地址to====》{}", toAdress);
				log.info("解析合约====》{}", s2);
				if (!this.contractAddress.equals(tron.getContractAddress())){
					tron.setCode(false);
				}
				return tron;
			} else if ("DEFAULT".equals(w)) {
				return getTransaction(txid);
			} else {
				tron.setCode(false);
				return tron;
			}
		} catch (Exception e) {
			log.error("处理查询订单失败，错误信息:{}", e.toString());
			tron.setCode(false);
			return tron;
		} finally {
			if (apiWrapper != null) {
				apiWrapper.close();
			}
		}
	}

	/**
	 * 根据交易id查询TRC20交易详情
	 *
	 * @param txid hash
	 * @return 详情
	 */
	public TronTransaction getTransactionTrx(String txid) {
		TronTransaction tron = new TronTransaction();
		ApiWrapper apiWrapper = null;
		try {
			apiWrapper = ApiWrapper.ofMainnet(privateKey, apiKey);
			Chain.Transaction transaction = null;

			//以下操作是防止签名交易了，但查询太快导致订单不存在
			Boolean flag = true;
			for (int i = 0; i < 10; i++) {
				if (flag) {
					try {
						transaction = apiWrapper.getTransactionById(txid);
						log.info("交易订单：==》" + transaction.toString());
						flag = false;
					} catch (IllegalException e) {
						log.error("查询订单失败，第【{}】次查询，错误信息:{}", i + 1, e.toString());
						flag = true;
					}
					Thread.sleep(1000L);
				}
			}

			if (transaction == null) {
				tron.setCode(false);
				return tron;
			}
			Chain.Transaction.Result result = transaction.getRet(0);
			String s = result.getContractRet().toString();
			if (s.equals("SUCCESS")) {
				Chain.Transaction.raw rawData = transaction.getRawData();
				Any parameter = rawData.getContract(0).getParameter();
				ByteString value = parameter.getValue();
				org.tron.trident.proto.Contract.TransferContract transferContract = org.tron.trident.proto.Contract.TransferContract.parseFrom(value.toByteArray());

				long amount = transferContract.getAmount();
				ByteString contractAddress = transferContract.getToAddress();
				ByteString ownerAddress = transferContract.getOwnerAddress();


				tron.setCode(true);
				tron.setOwnerAddress(Base58Check.bytesToBase58(ownerAddress.toByteArray()));
				tron.setContractAddress(Base58Check.bytesToBase58(contractAddress.toByteArray()));
				tron.setAmount(new BigDecimal(amount).divide(decimal));

				log.info("解析余额====》{}", new BigDecimal(amount).divide(decimal));
				log.info("to地址====》{}", contractAddress);
				log.info("from合约====》{}", ownerAddress);
				return tron;
			} else if ("DEFAULT".equals(s)) {
				return getTransactionTrx(txid);
			} else {
				tron.setCode(false);
				return tron;
			}
		} catch (Exception e) {
			log.error("处理查询订单失败，错误信息:{}", e.toString());
			tron.setCode(false);
			return tron;
		} finally {
			apiWrapper.close();
		}
	}


	/**
	 * 处理精度
	 *
	 * @param num
	 * @return
	 */
	private BigDecimal getNum(int num) {
		StringBuffer buffer = new StringBuffer("1");
		for (int i = 0; i < num; i++) {
			buffer.append("0");
		}
		return new BigDecimal(buffer.toString());
	}


}
