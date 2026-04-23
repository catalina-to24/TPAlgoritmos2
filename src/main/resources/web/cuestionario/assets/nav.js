(function(){
  var path = window.location.pathname.replace(/\/+$/,'') || '/cuestionario';
  document.querySelectorAll('.nav-link').forEach(function(a){
    var href = a.getAttribute('href');
    if(!href) return;
    if(href === path || (href !== '/cuestionario' && path.indexOf(href) === 0)){
      a.classList.add('active');
    }
    if(path === '/cuestionario' && href === '/cuestionario') a.classList.add('active');
  });

  var saved = localStorage.getItem('tpa2_theme') || 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  var btn = document.getElementById('theme-toggle');
  function label(){ return document.documentElement.getAttribute('data-theme') === 'dark' ? 'Modo claro' : 'Modo oscuro'; }
  if(btn){
    btn.textContent = label();
    btn.addEventListener('click', function(){
      var now = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', now);
      localStorage.setItem('tpa2_theme', now);
      btn.textContent = label();
    });
  }
})();
