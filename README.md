# tronDemo

波场官方 Java操作智能合约部分

前端交互请参考如下代码TronWeb
需要注意的是 转账trx时，如果余额不够会报异常
```vue
<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
 
    <button @click="linkWallet">
      连接钱包
    </button>
    <button @click="getBalance">
      获取钱包余额
    </button>
    <button @click="transaction">
      TRX转账交易
    </button>
    <button @click="transactionToken">
      TRX-ERC20转账交易
    </button>
    <button @click="transactionContract">
      合约方法调用
    </button>
  </div>
</template>
 
<script>
export default {
  name: 'HelloWorld',
  data () {
    return {
      msg: 'tron-web-dapp',
      tronWeb:null,
      walletAddress:null
    }
 
  },
  mounted () {
      if(window.tronWeb){
        this.tronWeb =  window.tronWeb;
        console.log(this.tronWeb)
        this.walletAddress = this.tronWeb.defaultAddress.base58;
      }
  },
  methods: {
    linkWallet(){
      if(window.tronWeb){
        this.tronWeb =  window.tronWeb;
        console.log(window.tronWeb)
        this.walletAddress = this.tronWeb.defaultAddress.base58;
        console.log(this.tronWeb.defaultAddress.base58)
      }
    },
    
    //查询钱包余额
    async getBalance()  {
        //当前连接的钱包地址获取 window.tronWeb.defaultAddress.base58
        var balance = await this.tronWeb.trx.getBalance(this.walletAddress);
        console.log("balance=",balance)
        
        
         //TRC20获取余额的方式
         var contractAddress = "合约地址";
         let contract = await this.tronWeb.then((res) => res.contract().at(contractAddress));
         let result = await contract.balanceOf(this.address).call();
         this.usdtBalance = Number(window.tronWeb.fromSun(result))
    },
    
    //trx转账交易
    async transaction() {
      var tx = await this.tronWeb.transactionBuilder.sendTrx(
        "TN9RRaXkCFtTXRso2GdTZxSxxwufzxLQPP",10 * Math.pow(10,6),this.walletAddress
      );
      //签名
      var signedTx = await this.tronWeb.trx.sign(tx);
      //广播
      var broastTx = await this.tronWeb.trx.sendRawTransaction(signedTx);
      console.log(broastTx)
    },
    
    //trx-token转账交易
    async transactionToken() {
 
      //转账方式1
 
     let contract = await this.tronWeb.contract().at("TURwoLuFy7maq1Vea7wVwRNz3HgmcAsJzb");
     let result = await contract.transfer(
        "TN9RRaXkCFtTXRso2GdTZxSxxwufzxLQPP",
        this.tronWeb.toHex(55 * Math.pow(10,18))
      ).send({
        feeLimit: 10000000
      }).then(output => {console.log('- Output:', output, '\n');});
      console.log('result: ', result);
 
      //转账方式2
      /*const parameter = [{type:'address',value:'TN9RRaXkCFtTXRso2GdTZxSxxwufzxLQPP'},{type:'uint256',value:this.tronWeb.toHex(25 * Math.pow(10,18))}]
      var tx  = await this.tronWeb.transactionBuilder.triggerSmartContract("TURwoLuFy7maq1Vea7wVwRNz3HgmcAsJzb", "transfer(address,uint256)", {},parameter,this.walletAddress);
      var signedTx = await this.tronWeb.trx.sign(tx.transaction);
      var broastTx = await this.tronWeb.trx.sendRawTransaction(signedTx);
      console.log(broastTx)*/
 
      /*let contract = await this.tronWeb.contract().at("TURwoLuFy7maq1Vea7wVwRNz3HgmcAsJzb");
     let result1 = await contract.decimals().call();
     console.log('result: ', result1);*/
    },
    //调用合约中的方法
    async transactionContract(){
 
      //调用方式1
      let contract = await this.tronWeb.contract().at("TSbJGFA8UyYGTuXBRbYB2GJeh2CY1X5F4d");
      console.log("contract=",contract)
      let result = await contract.registrationExt(
        "TN9RRaXkCFtTXRso2GdTZxSxxwufzxLQPP"
      ).send({
        callValue: this.tronWeb.toHex(25 * Math.pow(10,6)),//发送TRX余额
        feeLimit: 10000000
      }).then(output => {console.log('- Output:', output, '\n');});
      console.log('result: ', result)
 
      /*//调用方式2
      const parameter = [{type:'address',value:'TN9RRaXkCFtTXRso2GdTZxSxxwufzxLQPP'}];
      var tx  = await this.tronWeb.transactionBuilder.triggerSmartContract(
        "TSbJGFA8UyYGTuXBRbYB2GJeh2CY1X5F4d",
        "registrationExt(address)",
        {},
        parameter,
        this.walletAddress
      );*/
      
      //签名
      var signedTx = await this.tronWeb.trx.sign(tx.transaction);
      //广播
      var broastTx = await this.tronWeb.trx.sendRawTransaction(signedTx);
      console.log(broastTx)
    }
  }
}
</script>
 
<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>

 ```
