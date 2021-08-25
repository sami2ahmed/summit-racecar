<template>
  <div id="app">
    <div>
      <div>
        <p>
          <label>URL:</label>
          <input type="text" v-model="url" placeholder="http://">
        </p>

        <!-- <p>
          <label>Format:</label>
          <select v-model="format">
            <option value="plain">Plain</option>
            <option value="json">JSON</option>
          </select>
        </p> -->

        <!-- <p>
          <label>
            <input type="checkbox" v-model="includeCredentials">
            Include credentials?
          </label>
        </p> -->

        <p>
          <button :disabled="url.length === 0" @click.prevent="connect">Connect</button>
          <button @click.prevent="disconnect">Disconnect</button>
          <button @click.prevent="clear">Clear</button>
        </p>
      </div>

      <!-- <handlers
        :handlers.sync="handlers"
      /> -->
    </div>
    <div>
      <LogDisplayNew
        :logs="logs"
      />
    </div>
  </div>

</template>

<script lang="ts">
import Vue from 'vue'
import { SSEClient } from 'vue-sse/types'
import LogDisplayNew from './components/LogDisplayNew.vue'
import { now } from './utils'

let client: SSEClient | null

export default Vue.extend({
  name: 'App',
  components: {
    LogDisplayNew
  },
  data () {
    return {
      url: '/v1/api/sse?raceId=1',
      generateColumns: 'test',
      includeCredentials: false,
      format: 'json',
      handlers: [
        {
          event: 'message',
          color: '#00000'
        }
      ],
      logs: [] as [string, string, string][]
    }
  },
  methods: {
    connect () {
      // try disconnecting, just in case
      this.disconnect()

      this.log(`[info] connecting to ${this.url}`, 'system')

      // create the client with the user's config
      client = this.$sse.create({
        url: this.url,
        format: this.format,
        color: '#00000'
      //  includeCredentials: this.includeCredentials
      })

      // add the user's handlers
      this.handlers.forEach((h) => {
        client!.on(h.event, (data) => { // eslint-disable-line
          this.log(data, h.color)
        })
      })

      client!.on('error', () => { // eslint-disable-line
        this.log('[error] disconnected, automatically re-attempting connection', 'system')
      })

      // and finally -- try to connect!
      client!.connect() // eslint-disable-line
        .then(() => {
          this.log('[info] connected', 'system')
        })
        .catch(() => {
          this.log('[error] failed to connect', 'system')
        })
    },

    disconnect () {
      if (client) {
        client.disconnect()
        client = null
        this.log('[info] disconnected', 'system')
      }
    },

    clear () {
      this.logs = []
    },

    log (message: string, color: string) {
      console.log('heyo ', typeof message)
      // const formattedobject = JSON.parse(message)
      // console.log(formattedobject)
      this.logs.push([now(), message, color])
      console.log('array is ', this.logs)
      // console.info('parsed json', JSON.parse(message))
    }
  },
  beforeDestroy () {
    this.disconnect()
  }
})
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
}

h2 {
  display: inline;
}

button, input, select {
  font: inherit;
}

#app {
  background: #dddddd;
  font-family: sans-serif;
  min-height: 100vh;
  position: relative;
}

@media(min-width: 768px) {
  #app {
    display: flex;
  }

  #app > div {
    padding: 1rem;
  }

  #app > div:first-child {
    text-align: left;
  }

  #app > div:last-child {
    flex: 1;
  }
}
</style>
