<template>
  <div id="logs" ref="logs">
    <div>
      <table class="table table-striped table-bordered">
            <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Driver</th>
                    <th>Lap</th>
                    <th>Position</th>
                    <th>Surname</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(l, i) in logs" :key="i">
                    <td>{{l[0]}}</td>
                    <td>{{l[1]["driverId"]}}</td>
                    <td>{{l[1]["lap"]}}</td>
                    <td>{{l[1]["position"]}}</td>
                    <td>{{l[1]["surname"]}}</td>
                </tr>
            </tbody>
        </table>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'LogDisplayNew',
  props: {
    logs: {
      required: true,
      type: Array
    }
  },
  watch: {
    logs () {
      this.$nextTick(() => {
        const el = this.$refs.logs as Element
        el.scrollTop = el.scrollHeight
      })
    }
  }
})
</script>

<style>
.table>thead>tr>th {
  position: sticky;
  position: -webkit-sticky;
  top: 0;
  z-index: 2;
  background-color: white;
}

#logs {
  background: #333831;
  border-radius: 0.25rem;
  color: #e7e6e1;
  height: calc(100vh - 4rem);
  overflow-y: scroll;
  padding: 1rem;
}

#logs > div {
  padding-bottom: 0.5em;
}

#logs time {
  font-weight: bold;
  padding-right: 0.5em;
}

#logs span {
  padding-left: 0.5em;
}
</style>
