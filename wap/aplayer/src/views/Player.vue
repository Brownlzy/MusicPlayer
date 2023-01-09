<template>
  <div id="app">
    <!-- <br />
    <h4>歌曲来自App端<span>{{ selectCurriculums }}</span>列表</h4> -->
    <div id="webtitle">
      <h2>综音网页播放器</h2>
      <!-- 查看其他歌单内容，请使用下拉表单： -->
      歌曲来自App端
      <select v-model="selectCurriculums" @change="loadSongList()">
        <option v-for="(curriculum, index) in playlists" :key="index">{{ curriculum }}</option>
      </select>
      列表
    </div>
    <!-- 准备一个容器用来存放音乐播放器 -->
    <div id="aplayer"></div>
  </div>
</template>
<script>
import APlayer from "APlayer"; // 引入音乐插件
import "APlayer/dist/APlayer.min.css"; // 引入音乐插件的样式
import { getsongList, getInfo, getallSongList } from '../request/api.js'
var ap;
export default {
  name: "App",
  data() {
    return {
      selectCurriculums: '正在播放',
      playlists: ['正在播放'],
      audio: [], // 歌曲列表
      info: {},
    };
  },
  created() {
    this.loadInfo();
  },
  mounted() {
  },
  methods: {
    initAudio() {
      // 创建一个音乐播放器实例，并挂载到DOM上，同时进行相关配置
      ap = new APlayer({
        container: document.getElementById("aplayer"),
        audio: this.audio, // 音乐信息
        //mini: this.info.mini,
        // 其他配置信息
        ...this.info,
      });
    },
    loadSongList() {
      getsongList({
        name: this.selectCurriculums
      }).then(res => {
        this.audio = res.data.data.audio;
        //console.log(this.audio);
        if (ap == null)
          this.initAudio();
        else {
          ap.list.clear();
          ap.list.add(this.audio);
        }
      })
    },
    loadInfo() {
      getInfo().then(res => {
        this.info = res.data.data;
        //console.log(this.info);
        this.loadAllSongList();
      })
    },
    loadAllSongList() {
      getallSongList().then(res => {
        this.playlists = res.data.data.playlists;
        //console.log(this.playlists);
        this.loadSongList();
      })
    },
  },
};
</script>
<style lang="less" scoped>
#app {
  width: 100%;
  height: 100%;
  // padding: 50px;
  opacity: 0.9;

  @media (min-width: 1024px) {
    #webtitle {
      background-color: aliceblue;
      padding: 5px;
      margin: 5px;
      border-radius: 3px;
      width: 480px; // 定个宽度
    }

    #aplayer {
      background-color: aliceblue;
      width: 480px; // 定个宽度
    }
  }
  @media (max-width: 1024px) {
    #webtitle {
      background-color: aliceblue;
      padding: 5px;
      margin: 5px;
      border-radius: 3px;
    }

    #aplayer {
      background-color: aliceblue;
    }
  }
}
</style>
