<template>
  <div id="app">
    <h2>综音网页播放器</h2>
    <br />
    <h4>歌曲来自App端正在播放列表</h4>
    <br />
    <!-- 准备一个容器用来存放音乐播放器 -->
    <div id="aplayer"></div>
  </div>
</template>
<script>
import APlayer from "APlayer"; // 引入音乐插件
import "APlayer/dist/APlayer.min.css"; // 引入音乐插件的样式
import { getsongList, getInfo } from '../request/api.js'
export default {
  name: "App",
  data() {
    return {
      audio: [ // 歌曲列表
      ],
      info: {
        mini: false,
        autoplay: false,
        theme: '#FADFA3',
        loop: 'all',
        order: 'random',
        preload: 'auto',
        volume: 0.7,
        mutex: true,
        listFolded: false,
        listMaxHeight: 90,
        lrcType: 3,
      }
    };
  },
  created() {
    window.myData = this;
  },
  mounted() {
    // this.loadInfo();
    this.loadSongList();
    //this.startMusic();
    window.vue = this;

  },
  methods: {
    startMusic() {
      Promise.all([this.loadInfo(), this.loadSongList()]).then(() => {
        // 初始化播放器
        //this.initAudio();
        console.log('我是created中的事件，现在两个接口都执行完毕啦')
      })
    },
    initAudio() {
      // 创建一个音乐播放器实例，并挂载到DOM上，同时进行相关配置
      const ap = new APlayer({
        container: document.getElementById("aplayer"),
        audio: this.audio, // 音乐信息

        // mini: false,
        // autoplay: false,
        // theme: '#FADFA3',
        // loop: 'all',
        // order: 'random',
        // preload: 'auto',
        // volume: 0.7,
        // mutex: true,
        // listFolded: false,
        // listMaxHeight: 90,
        // lrcType: 3,

        mini: this.info.mini,
        autoplay: this.info.autoplay,
        theme: this.info.theme,
        loop: this.info.loop,
        order: this.info.order,
        preload: this.info.preload,
        volume: this.info.volume,
        mutex: this.info.mutex,
        listFolded: this.info.listFolded,
        listMaxHeight: this.info.listMaxHeight,
        lrcType: this.info.lrcType,

        // 其他配置信息
      });
    },
    loadSongList() {
      getsongList({
        name: "playingList"
      }).then(res => {
        this.audio = res.data.data.audio;
        console.log(this.audio);
        this.loadInfo();
      })
    },
    loadInfo() {
      getInfo().then(res => {
        this.info = res.data.data;
        console.log(this.info);
        this.initAudio();
      })
    },
  },
};
</script>
<style lang="less" scoped>
@media (min-width: 1024px) {
  .about {
    min-height: 100vh;
    display: flex;
    align-items: center;
  }
}

#app {
  width: 100%;
  height: 100%;
  padding: 50px;

  #aplayer {
    width: 480px; // 定个宽度
  }
}
</style>
