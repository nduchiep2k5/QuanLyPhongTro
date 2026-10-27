document.querySelectorAll('[data-confirm]').forEach(f=>f.addEventListener('submit',e=>{if(!confirm(f.dataset.confirm))e.preventDefault()}));
document.querySelectorAll('[data-page]').forEach(b=>b.addEventListener('click',()=>{const url=new URL(location.href);url.searchParams.set('page',b.dataset.page);location.assign(url)}));
document.querySelectorAll('[data-back]').forEach(b=>b.addEventListener('click',()=>history.length>1?history.back():location.assign('/')));
const search=document.querySelector('[data-table-search]');if(search)search.addEventListener('input',()=>{document.querySelectorAll('tbody tr').forEach(r=>r.hidden=!r.textContent.toLocaleLowerCase('vi').includes(search.value.toLocaleLowerCase('vi')))});
const files=document.getElementById('room-files');if(files){let urls=[];files.addEventListener('change',()=>{urls.forEach(URL.revokeObjectURL);urls=[];const out=document.getElementById('file-preview');out.replaceChildren();for(const f of files.files){if(!['image/jpeg','image/png'].includes(f.type))continue;const img=document.createElement('img');img.alt='Ảnh xem trước';img.src=URL.createObjectURL(f);urls.push(img.src);out.append(img)}})}
const chatForm=document.getElementById('chat-form');if(chatForm){
  const messages=document.getElementById('chat-messages'),input=document.getElementById('chat-question'),submit=chatForm.querySelector('button[type=submit]'),history=[];
  function addMessage(role,text,rooms=[]){const item=document.createElement('div');item.className='chat-message '+role;const body=document.createElement('span');body.textContent=text;item.append(body);for(const room of rooms){if(!Number.isSafeInteger(room.id)||room.url!==`/rooms/${room.id}`)continue;const link=document.createElement('a');link.href=room.url;link.textContent=room.title;item.append(link)}messages.append(item);messages.scrollTop=messages.scrollHeight}
  document.getElementById('chat-clear').addEventListener('click',()=>{history.length=0;messages.replaceChildren();input.focus()});
  chatForm.addEventListener('submit',async e=>{e.preventDefault();const question=input.value.trim();if(!question||submit.disabled)return;submit.disabled=true;input.value='';addMessage('user',question);try{
    const token=document.querySelector('meta[name="_csrf"]'),header=document.querySelector('meta[name="_csrf_header"]');
    const response=await fetch('/api/chat',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json',[header.content]:token.content},body:JSON.stringify({question,history}),signal:AbortSignal.timeout(20000)});
    const result=await response.json();if(!response.ok)throw new Error(result.error||'Không thể kết nối chatbot.');
    addMessage('assistant',result.answer,result.rooms);history.push({role:'user',content:question},{role:'assistant',content:result.answer});if(history.length>8)history.splice(0,2)
  }catch(error){addMessage('assistant',error.message||'Không thể kết nối chatbot. Vui lòng thử lại.')}finally{submit.disabled=false;input.focus()}})
}
