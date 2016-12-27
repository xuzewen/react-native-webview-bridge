(function () {
  'use strict'

  var ids = 0
  var invokers = {}
  var responseCallbacks = {}

  //invoker has accept 2 arguments
  //fn(args, result). result is a function which accept an argument.
  //that argument is being used to send to caller as a result value.
  //by using this function, we can support both sync and async operation
  function register(sender, name, fn) {
    invokers[name] = function (id, args) {
      fn(args, function (result) {
        sender({
          id: id,
          type: 'resolve',
          result: result
        })
      }, function (err) {
        sender({
          id: id,
          type: 'reject',
          result: err
        })
      })
    }
  }

  function invoke(sender, name, args, opt, callback) {
    var id = ++ids
    var target = {
      callback: callback
    }

    if (!opt) {
      opt = {}
    }

    if (!opt.timeout) {
      opt.timeout = 0
    }

    if (opt.timeout) {
      target.timeoutHandler = setTimeout(function () {
        onReject(id, 'timeout')
      }, opt.timeout)
    }

    responseCallbacks[id] = target

    sender({
      id: id,
      type: 'invoke',
      name: name,
      args: args
    })
  }

  function onInvoke(payload) {
    var invoker = invokers[payload.name]
    if (invoker) {
      setTimeout(function () {
        invoker(payload.id, payload.args)
      }, 15)
    }
  }

  function onResolve(id, result) {
    var target = responseCallbacks[id]
    if (target) {
      clearTimeout(target.timeoutHandler)
      delete responseCallbacks[id]
      setTimeout(function () {
        target.callback(null, result)
      }, 15)
    }
  }

  function onReject(id, result) {
    var target = responseCallbacks[id]
    if (target) {
      clearTimeout(target.timeoutHandler)
      delete responseCallbacks[id]
      setTimeout(function () {
        target.callback(result, null)
      }, 15)
    }
  }

  function onMessage(payload) {
    if (typeof payload === 'string') {
      return
    }

    // there are two types of payload
    // invoke: { type: 'payload', id, name, args }
    // resolve: { type: 'resolve', id, result }
    // reject: { type: 'reject', id, result }
    switch(payload.type) {
      case 'invoke':
        onInvoke(payload)
        break
      case 'resolve':
        onResolve(payload.id, payload.result)
        break
      case 'reject':
        onReject(payload.id, payload.result)
      default:
        //ignore
    }
  }

  //init this method register and attaches rpc to WebViewBridge.
  //you can either check whether WebViewBridge.rpc is availebe or
  //simply register to `webviewbridge:rpc` event.
  function init(WebViewBridge) {
    var rpc = {}
    var sender = WebViewBridge.send

    window.removeEventListener('webviewbridge:init', init)
    WebViewBridge.addMessageListener(onMessage)

    rpc.register = function (name, fn) {
      register(sender, name, fn)
    }

    rpc.invoke = function (name, args, opt, callback) {
      invoke(sender, name, args, opt, callback)
    }

    WebViewBridge.rpc = rpc
    WebViewBridge.__dispatch__('webviewbridge:rpc', rpc)
  }

  if (window.WebViewBridge) {
    init(window.WebViewBridge)
  } else {
    window.addEventListener('webviewbridge:init', init)
  }
}())
