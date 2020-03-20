/**
 * 
 * Manipulating the DOM exercise.
 * Exercise programmatically builds navigation,
 * scrolls to anchors from navigation,
 * and highlights section in viewport upon scrolling.
 * 
 * Dependencies: None
 * 
 * JS Version: ES2015/ES6
 * 
 * JS Standard: ESlint
 * 
*/

/**
 * Define Global Variables
 * 
*/

let ul = document.getElementById('navbar__list')
let section = document.querySelectorAll("section")

/**
 * End Global Variables
 * Start Helper Functions
 * 
*/



/**
 * End Helper Functions
 * Begin Main Functions
 * 
*/

// build the nav 
buildNav = function(){
    const re = document.createElement('li')
    re.classList.add('nav_section')
    re.setAttribute('id','returnTop')
    re.style.display = 'none'
    re.textContent = 'TOP'
    ul.appendChild(re)
    for (let i=0;i<=3;i++){
        let tag = section[i].getElementsByTagName('h2')
        let content = tag[0].textContent
        let li = document.createElement('li')
        li.classList.add('nav_section')
        if(i==0){
            li.setAttribute('id','nav-section1')
        }
        else if(i==1){
            li.setAttribute('id','nav-section2')
        }
        else if(i==2){
            li.setAttribute('id','nav-section3')
        }
        else{
            li.setAttribute('id','nav-section4')
        }
        li.textContent=content
        ul.appendChild(li)
    
    }
}

// Add class 'active' to section when near top of viewport
chooseAvtive=function(){
    let top = window.scrollY  + window.screen.availHeight/2
    let listLi = ul.getElementsByTagName('li')
    if (top >section[0].offsetTop && top <section[0].offsetHeight + section[0].offsetTop ){
        for(let i=0;i<=3;i++){
            if(i==0){
                section[i].classList.add('active')
            }
            else{
                section[i].classList.remove('active')
            }
        }
        for(let i=1;i<=4;i++){
            if(i==1){
                listLi[i].style.background='lightblue'
            }
            else{
                listLi[i].style.background=''
            }
        }
    }
    else if(top >section[1].offsetTop && top <section[1].offsetHeight + section[1].offsetTop){
        for(let i=0;i<=3;i++){
            if(i==1){
                section[i].classList.add('active')
            }
            else{
                section[i].classList.remove('active')
            }
        }
        for(let i=1;i<=4;i++){
            if(i==2){
                listLi[i].style.background='lightblue'
            }
            else{
                listLi[i].style.background=''
            }
        }
    }
    else if(top >section[2].offsetTop && top <section[2].offsetHeight + section[2].offsetTop ){
        for(let i=0;i<=3;i++){
            if(i==2){
                section[i].classList.add('active')
            }
            else{
                section[i].classList.remove('active')
            }
        }
        for(let i=1;i<=4;i++){
            if(i==3){
                listLi[i].style.background='lightblue'
            }
            else{
                listLi[i].style.background=''
            }
        }
    }
    else if(top >section[3].offsetTop && top <section[3].offsetHeight + section[3].offsetTop ){
        for(let i=0;i<=3;i++){
            if(i==3){
                section[i].classList.add('active')
            }
            else{
                section[i].classList.remove('active')
            }
        }
        for(let i=1;i<=4;i++){
            if(i==4){
                listLi[i].style.background='lightblue'
            }
            else{
                listLi[i].style.background=''
            }
        }
    }
    else{
        for(let i=0;i<=3;i++){
            section[i].classList.remove('active')
        }
        for(let i=1;i<=4;i++){
            listLi[i].style.background=''
        }
    }
}

// Scroll to anchor ID using scrollTO event
scrollToSection1=function(){
    section[0].scrollIntoView({behavior:'smooth'})
}
scrollToSection2=function(){
    section[1].scrollIntoView({behavior:'smooth'})
}
scrollToSection3=function(){
    section[2].scrollIntoView({behavior:'smooth'})
}
scrollToSection4=function(){
    section[3].scrollIntoView({behavior:'smooth'})
}
// scrollToTop=function(){
//     ul.scrollIntoView({behavior:'smooth'})
//     // window.scrollTo(0,0)
    
// }
const drag = 10;
// 滑动到顶部
const scrollToTop = () => {
// 距离顶部的距离
  const gap = document.documentElement.scrollTop || document.body.scrollTop;
  if (gap > 0) {
    window.requestAnimationFrame(scrollToTop);
    window.scrollTo(0, gap - gap / drag);
  }
};



/**
 * End Main Functions
 * Begin Events
 * 
*/

// Build menu 
buildNav()

// Scroll to section on link click
const navSection1 = document.getElementById('nav-section1')
const navSection2 = document.getElementById('nav-section2')
const navSection3 = document.getElementById('nav-section3')
const navSection4 = document.getElementById('nav-section4')
const navTop = document.getElementById('returnTop')
navSection1.addEventListener('click',function(){
    scrollToSection1()
})
navSection2.addEventListener('click',function(){
    scrollToSection2()
})
navSection3.addEventListener('click',function(){
    scrollToSection3()
})
navSection4.addEventListener('click',function(){
    scrollToSection4()
})
navTop.addEventListener('click',function(){
    scrollToTop()
})
// Set sections as active and hide menu
let test1=setTimeout(function(){
    ul.style.display='none'
},3000)
window.onscroll = function(){
    window.clearTimeout(test1)
    ul.style.display=''
    chooseAvtive()
    if(window.scrollY + this.document.documentElement.clientHeight >= this.document.documentElement.offsetHeight-100){
        navTop.style.display=''
    }
    else{
        navTop.style.display='none'
    }
    let test2=setTimeout(function(){
        ul.style.display='none'
    },3000)
    test1=test2
}
//

